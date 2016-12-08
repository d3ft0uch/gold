package org.akkatrading.live

import java.time.ZonedDateTime

import akka.actor._
import akka.util.Timeout
import org.akkatrading.live.CandleFetcher._
import org.akkatrading.live.util.DateUtils._
import org.akkatrading.live.util.{TimeFrameEnum, InstrumentEnum}
import org.akkatrading.live.util.InstrumentEnum.InstrumentVal
import org.akkatrading.live.util.Quotes.FullCandle
import org.akkatrading.live.util.TimeFrameEnum.TimeFrameVal
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding.{Deflate, Gzip}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object CandleFetcher {

  def props(hostConnector: ActorRef, orderManager: ActorRef): Props = Props(new CandleFetcher(hostConnector, orderManager))

  case class FetchCandles(instrument: InstrumentVal, count: Int, granularity: TimeFrameVal, candleFormat: String)

  case class PriceUpdate(instrument: InstrumentVal, granularity: TimeFrameVal, current: Candle, previous: Candle)

  case class BulkPriceUpdate(instrument: InstrumentVal, granularity: TimeFrameVal, candles: List[Candle])

  case class Candle(time: ZonedDateTime, openBid: Double, openAsk: Double, highBid: Double, highAsk: Double, lowBid: Double, lowAsk: Double, closeBid: Double, closeAsk: Double, volume: Int, complete: Boolean) {

    def canEqual(other: Any): Boolean = other.isInstanceOf[Candle] || other.isInstanceOf[FullCandle]

    def equalsPrices(other: Any): Boolean = other match {
      case that: Candle =>
        (that canEqual this) &&
          time == that.time &&
          openBid == that.openBid &&
          openAsk == that.openAsk &&
          highBid == that.highBid &&
          highAsk == that.highAsk &&
          lowBid == that.lowBid &&
          lowAsk == that.lowAsk &&
          closeBid == that.closeBid &&
          closeAsk == that.closeAsk
      case that: FullCandle =>
        (that canEqual this) &&
          time == that.time &&
          openBid == that.openBid &&
          openAsk == that.openAsk &&
          highBid == that.highBid &&
          highAsk == that.highAsk &&
          lowBid == that.lowBid &&
          lowAsk == that.lowAsk &&
          closeBid == that.closeBid &&
          closeAsk == that.closeAsk
      case _ => false
    }

  }

  case class CandleResponse(instrument: String, granularity: String, candles: List[Candle])

  object CandleJsonProtocol extends DefaultJsonProtocol {
    implicit val candleFmt = jsonFormat11(Candle)
    implicit val candleResponseFmt = jsonFormat3(CandleResponse)
  }

}

class CandleFetcher(connector: ActorRef, orderManager: ActorRef) extends Actor with ActorLogging with AuthInfo {

  import context.dispatcher
  import org.akkatrading.live.CandleFetcher.CandleJsonProtocol._

  implicit val timeout: Timeout = Timeout(15 seconds)

  for (instrument <- instruments; timeFrame <- timeFrames) {
    context.system.scheduler.schedule(0 milliseconds, 500 milliseconds, self, FetchCandles(instrument, 2, timeFrame, "bidask"))
  }

  val pipeline: HttpRequest => Future[CandleResponse] = (
    addCredentials(OAuth2BearerToken(authToken))
      ~> encode(Gzip)
      ~> sendReceive(connector)
      ~> decode(Deflate)
      ~> unmarshal[CandleResponse]
    )

  def receive = {
    case FetchCandles(instrument, count, granularity, candleFormat) =>
      val response: Future[CandleResponse] =
        pipeline(Get(s"/v1/candles?instrument=$instrument&count=$count&candleFormat=$candleFormat&granularity=$granularity"))
      response onComplete {
        case Success(CandleResponse(_, _, candles)) =>
          log.debug("Fetched candles: {}", candles)
          if (count == 2) {
            val priceUpdate = PriceUpdate(instrument, granularity, candles(1), candles.head)
            orderManager ! priceUpdate
          } else {
            log.info(s"Fetched bulk price update for $instrument $granularity: size:${candles.length}")
            val bulkPriceUpdate = BulkPriceUpdate(instrument, granularity, candles)
            orderManager ! bulkPriceUpdate
          }

        case Success(somethingUnexpected) =>
          log.warning("The Oanda API call was successful but returned something unexpected: '{}'.", somethingUnexpected)

        case Failure(error) =>
          log.error(error, "Couldn't fetch candles")
      }
    case other => log.info("Received something unexpected: {}", other)
  }
}
