package org.akkatrading.live

import java.lang.Exception
import java.time.ZonedDateTime
import akka.actor.{ActorRef, FSM, Props}
import org.akkatrading.backtest.model.CandleWithIndicators
import org.akkatrading.live.CandleFetcher.{BulkPriceUpdate, FetchCandles, PriceUpdate}
import org.akkatrading.live.OrderCreator.{CreateOrderConfirmation, CreateOrderRequest}
import org.akkatrading.live.OrderFetcher.FetchOrderRequest
import org.akkatrading.live.PriceListener.Tick
import org.akkatrading.live.StrategyFSM._
import org.akkatrading.live.util.InstrumentEnum.InstrumentVal
import org.akkatrading.live.util.{OrderType, Side, TimeFrameEnum, Quotes}
import org.goldmine.functions.{DMFunction, ADXFunction, Laguerre, EMAFunction}
import org.goldmine.indicator.Factor
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.util.control.Exception
import org.akkatrading.live.OrderCanceler.CancelOrderRequest
import org.akkatrading.live.OrderCanceler.CancelOrderConfirmation
import org.akkatrading.live.OrderCreator.CreateOrderConfirmation
import org.akkatrading.live.OrderCanceler.CancelOrderRequest
import org.akkatrading.live.OrderFetcher.Trade
import org.akkatrading.live.OrderFetcher.FetchTradeResponse
import org.akkatrading.live.OrderCanceler.CancelOrderFailure
import org.akkatrading.live.OrderCreator.CreateOrderFailure

object StrategyFSM {

  def props(connector: ActorRef): Props = Props(new StrategyFSM(connector))

  sealed trait State

  case object BeforeStart extends State

  case object DownloadingMissingCandles extends State

  case object Ready extends State

  case object SendingOrder extends State

  case object Trading extends State

  sealed trait Data

 

  final case class OrderToSend(createOrderRequest: CreateOrderRequest, orders: List[Trade]) extends Data
  
  final case class OrderToClose(cancelOrderRequest: CancelOrderRequest, orders: List[Trade]) extends Data

  final case class OpenedTrades(orders: List[Trade]) extends Data

}

class StrategyFSM(connector: ActorRef) extends FSM[State, Data] with AuthInfo {

  implicit val timeout: akka.util.Timeout = akka.util.Timeout(15 seconds)

  val orderCreator = context.actorOf(OrderCreator.props(connector), "orderCreator")
  val orderCanceler = context.actorOf(OrderCanceler.props(connector), "orderCanceler")
  val tradeModifier = context.actorOf(TradeModifier.props(connector), "tradeModifier")
  val candleFetcher = context.actorOf(CandleFetcher.props(connector, self), "candleFetcher")
  val orderFetcher = context.actorOf(OrderFetcher.props(connector), "orderFetcher")

  val periodDi = new Factor("Period Di", 14)
  val periodAdx = new Factor("Period Adx", 14)

  val preloadCandlesCount = 50 // Maximum candles sufficient for indicators

  Quotes.init(instruments, timeFrames)

  startWith(BeforeStart, new OpenedTrades(List.empty))

  when(BeforeStart) {
    case Event(priceUpdate: PriceUpdate, data: OpenedTrades) =>
      Quotes.handlePriceUpdate(priceUpdate)
      if (!Quotes.enoughCandles(priceUpdate.instrument, priceUpdate.granularity, preloadCandlesCount)) {
        log.info(s"Insufficient candles for ${priceUpdate.instrument} ${priceUpdate.granularity}. Need ${preloadCandlesCount}, downloading...")
        candleFetcher ! FetchCandles(priceUpdate.instrument, preloadCandlesCount, priceUpdate.granularity, "bidask")
        
        goto(DownloadingMissingCandles)
      } 
      else {
        log.info("Sending orderFetcher in BeforeStart stateData={}", stateData)  
        orderFetcher ! FetchOrderRequest()
          stay()
      } 
    case Event(tradeResponse : FetchTradeResponse, data) => 
      log.info("Ready to trade!")
      goto(Ready) using new OpenedTrades(tradeResponse.trades)
    
    case _ =>
      log.info("Transition to Ready State Failed! stateData={}", stateData)
      stay()
  }

  when(DownloadingMissingCandles) {
    case Event(bulkPriceUpdate: BulkPriceUpdate, data) =>
      Quotes.handleBulkPriceUpdate(bulkPriceUpdate)
      log.info("This is the current stateData={}", stateData)
      goto(BeforeStart)
    case _ =>
      log.info("Transition to SendingOrder Failed! stateData={}", stateData)
      stay()
  }

  when(Ready) {
    case Event(bulkPriceUpdate: BulkPriceUpdate, data) =>
      Quotes.handleBulkPriceUpdate(bulkPriceUpdate)
      stay()
    case Event(priceUpdate: PriceUpdate, data: OpenedTrades) =>
      val start = System.nanoTime()
      Quotes.handlePriceUpdate(priceUpdate)
      think(start, data)
    case Event(tradeResponse : FetchTradeResponse, data) => 
      stay() using new OpenedTrades(tradeResponse.trades)
    case Event(tick: Tick, data: OpenedTrades) =>
      val start = System.nanoTime()
      Quotes.handleTick(tick)
      think(start, data)
  }

  when(SendingOrder) {
    case Event(сreateOrderConfirmation: CreateOrderConfirmation, ots: OrderToSend) =>
      log.info("Sending orderFetcher in SendingOrder got create stateData={}", stateData)
      orderFetcher ! FetchOrderRequest()
      stay()
    case Event(сreateOrderFailure: CreateOrderFailure, ots: OrderToSend) =>
      log.info("Sending orderFetcher in SendingOrder got create failure stateData={}", stateData)
      orderFetcher ! FetchOrderRequest()
      stay()
    case Event(cancelOrderConfirmation: CancelOrderConfirmation, otc: OrderToClose) =>
      log.info("Sending orderFetcher in SendingOrder in cancelOrder stateData={}", stateData)
      orderFetcher ! FetchOrderRequest()
       stay()
    case Event(cancelOrderFailure: CancelOrderFailure, otc: OrderToClose) =>
      log.info("Sending orderFetcher in SendingOrder cancel failure stateData={}", stateData)
      orderFetcher ! FetchOrderRequest()
       stay()
    case Event(tradeResponse : FetchTradeResponse, ots: OrderToSend) => 
      goto(Trading) using new OpenedTrades(tradeResponse.trades)
    case Event(tradeResponse : FetchTradeResponse, otc: OrderToClose) => 
      goto(Trading) using new OpenedTrades(tradeResponse.trades)
    case Event(bulkPriceUpdate: BulkPriceUpdate, data) =>
      Quotes.handleBulkPriceUpdate(bulkPriceUpdate)
      log.info("in bulkPriceUpdate :  stateData={}", stateData)
      stay()
    case Event(priceUpdate: PriceUpdate, data) =>
      log.info("in priceUpdate :  stateData={}", stateData)
      Quotes.handlePriceUpdate(priceUpdate)
      stay()
    case Event(tick: Tick, data) =>
      log.info("in tick :  stateData={}", stateData)
      Quotes.handleTick(tick)
      stay()
  }

  when(Trading) {
    case Event(bulkPriceUpdate: BulkPriceUpdate, data) =>
      log.info("in bulkPriceUpdate :  stateData={}", stateData)
      Quotes.handleBulkPriceUpdate(bulkPriceUpdate)
      stay()
    case Event(tradeResponse : FetchTradeResponse, data) => 
      log.info("in tradeResponse :  stateData={}", stateData)
      stay() using new OpenedTrades(tradeResponse.trades)
    case Event(priceUpdate: PriceUpdate, openedTrades: OpenedTrades) =>
      
      val start = System.nanoTime()
      Quotes.handlePriceUpdate(priceUpdate)
      think(start, openedTrades)
    case Event(tick: Tick, openedTrades: OpenedTrades) =>
      val start = System.nanoTime()
      Quotes.handleTick(tick)
      think(start, openedTrades)
  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }

  def sendOrder(nextStateData: Data) = {
    nextStateData match {
      case OrderToSend(createOrderRequest, orders) =>
        log.info("Transition to SendingOrder. createOrderRequest={}", createOrderRequest)
        orderCreator ! createOrderRequest
      case OrderToClose(cancelOrderRequest, orders) =>
        log.info("Transition to SendingOrder. cancelOrderRequest={}", cancelOrderRequest)
        orderCanceler ! cancelOrderRequest
      case _ =>
        log.info("Transition to SendingOrder Failed! stateData={}", stateData)
    }
  }

  onTransition {
    case Ready -> SendingOrder =>
      sendOrder(nextStateData)
    case Trading -> SendingOrder =>
      sendOrder(nextStateData)
  }

  initialize()


  def think(start: Long, openedTrades: OpenedTrades) = {

    val m1TimeFrame = Quotes.getQuotes(instruments.head, TimeFrameEnum.M1)
    val m1Slice = m1TimeFrame.getSlice(preloadCandlesCount)

    val m5TimeFrame = Quotes.getQuotes(instruments.head, TimeFrameEnum.M5)
    val m5Slice = m5TimeFrame.getSlice(preloadCandlesCount)

    //val m1OpenSlice = m1Slice.map(_.open).toArray
    val m1HighSlice = m1Slice.map(_.high).toArray
    val m1LowSlice = m1Slice.map(_.low).toArray
    val m1CloseSlice = m1Slice.map(_.close).toArray

    //m5
    val m5HighSlice = m5Slice.map(_.high).toArray
    val m5LowSlice = m5Slice.map(_.low).toArray
    val m5CloseSlice = m5Slice.map(_.close).toArray

    val adxFunctionM1 = new ADXFunction(m1HighSlice, m1LowSlice, m1CloseSlice, periodDi, periodAdx)
    val adxFunctionM5 = new ADXFunction(m5HighSlice, m5LowSlice, m5CloseSlice, periodDi, periodAdx)

    val adxResultM1 = adxFunctionM1.adx(m1CloseSlice.length-1)
    val adxResultM5 = adxFunctionM5.adx(m5CloseSlice.length-1)
    val adx = adxResultM5.adx
    val adxPrev = adxResultM5.adxPrev
    val plusDI = adxResultM5.plusDi
    val minusDI = adxResultM5.minusDi

    /*if (adxMainArray[2] < adxMainArray[1] && adxMinusDIArray[1] < adxPlusDIArray[1]) buyOrSellSignal = 1;
    else
    if (adxMainArray[2] < adxMainArray[1] && adxMinusDIArray[1] > adxPlusDIArray[1]) buyOrSellSignal = -1;*/

    log.debug("M1=%s; M1Prev=%s; M5=%s; M5Prev=%s; adx=%f, metric=%dms".format(
      m1TimeFrame.lastCandle, m1TimeFrame.quotes(m1TimeFrame.quotes.length-2), m5TimeFrame.lastCandle, m5TimeFrame.quotes(m5TimeFrame.quotes.length-2),
      adx,
      (System.nanoTime()-start)/1000000))

    //Do nothing by default if you don't know what to do
    var toDo = stay()

    log.info("adx={}; adxPrev={};", adx, adxPrev)
    if (adx > adxPrev) {

      val buyOpened = openedTrades.orders.exists(o=>o.instrument == instruments.head.toString() && o.side == Side.buy.toString())
      val sellOpened = openedTrades.orders.exists(o=>o.instrument == instruments.head.toString() && o.side == Side.sell.toString())
      val buyOrderOption = openedTrades.orders.find(o=>o.instrument == instruments.head.toString() && o.side == Side.buy.toString())
      val sellOrderOption = openedTrades.orders.find(o=>o.instrument == instruments.head.toString() && o.side == Side.sell.toString())

      if (minusDI < plusDI) {
        //buy signal!

        //Do not open new buy order if we already have buy order opened
        if (!buyOpened) {
          val createOrderRequest = CreateOrderRequest(instruments.head, 1, Side.buy, OrderType.market, None, None, None, None)
          log.info("Starting to buy")
          toDo = goto(SendingOrder) using OrderToSend(createOrderRequest, openedTrades.orders)
        } else {
          if (sellOpened && !sellOrderOption.isEmpty) {
            //close sell order
             val cancelOrderRequest = CancelOrderRequest(sellOrderOption.get.id, sellOrderOption.get.units) 
            toDo = goto(SendingOrder) using OrderToClose(cancelOrderRequest, openedTrades.orders)
          }
        }

      } 
      else if (minusDI > plusDI) {
        //sell signal!

        //Do not open new buy order if we already have buy order opened
        if (!sellOpened) {
          val createOrderRequest = CreateOrderRequest(instruments.head, 1, Side.sell, OrderType.market, None, None, None, None)
          log.info("Starting to sell")
          toDo = goto(SendingOrder) using OrderToSend(createOrderRequest, openedTrades.orders)
        } 
        else {

          if (buyOpened && !buyOrderOption.isEmpty) {
            val cancelOrderRequest = CancelOrderRequest(buyOrderOption.get.id, buyOrderOption.get.units) 
            toDo = goto(SendingOrder) using OrderToClose(cancelOrderRequest, openedTrades.orders)
          }
        }
      }
    }

    toDo //final decision
  }

}

