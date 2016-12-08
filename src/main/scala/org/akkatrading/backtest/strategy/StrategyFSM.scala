package org.akkatrading.backtest.strategy

import akka.actor.{ActorLogging, Actor, FSM}
import org.akkatrading.backtest.model.{CandleWithIndicators}
import org.akkatrading.backtest.strategy.StrategyFSM._
import org.goldmine.functions._
import org.goldmine.indicator.Factor

import scala.collection.mutable.ArrayBuffer

object StrategyFSM {

  sealed trait State

  case object Flat extends State

  sealed trait Data

  case object Empty extends Data

}

class StrategyFSM extends Actor with FSM[State, Data] with ActorLogging {

  startWith(Flat, Empty)

  val O = new ArrayBuffer[Double]
  val H = new ArrayBuffer[Double]
  val L = new ArrayBuffer[Double]
  val C = new ArrayBuffer[Double]

  when(Flat) {
    case Event(tick: CandleWithIndicators, data) =>
      O += tick.open
      H += tick.high
      L += tick.low
      C += tick.close

      val period5 = new Factor("Period 5 EMA", 5)
      val period60 = new Factor("Period 60 EMA", 60)
      val period200 = new Factor("Period 200 EMA", 200)

      val openSlice = O.toArray //quotes.toArray.slice(quotes.toArray.length - period*2, quotes.toArray.length)
      val highSlice = H.toArray
      val lowSlice = L.toArray
      val closeSlice = C.toArray

      val ema5 = new EMAFunction(closeSlice, period5)
      val ema60 = new EMAFunction(closeSlice, period60)
      val ema200 = new EMAFunction(closeSlice, period200)

      val periodDi = new Factor("Period Di", 14)
      val periodAdx = new Factor("Period Adx", 14)
      //val adxMain = new ADXFunction(highSlice, lowSlice, closeSlice, periodDi, periodAdx)
      //val dxMain = new DXFunction(highSlice, lowSlice, closeSlice, periodDi)
      val diMain = new DIFunction(highSlice, lowSlice, closeSlice, periodDi)
      val dmFunction = new DMFunction(highSlice, lowSlice, closeSlice)
      val trFunction = new TRFunction(highSlice, lowSlice, closeSlice)
      val dxFunction = new DXFunction(highSlice, lowSlice, closeSlice, periodDi)
      val adxFunction = new ADXFunction(highSlice, lowSlice, closeSlice, periodDi, periodAdx)

      if (closeSlice.length > 1) {
        //; adxPlusDi=%f; adxPlusDiPrev=%f; adxMinusDi=%f; adxMinusDiPrev=%f; adxMain=%f; adxMainPrev=%f; atr=%f; atrPrev=%f; atr1Hour=%f; atr1HourPrev=%f
        log.info(s"I've got a candle! $tick; " +
          s"ema5=${ema5.compute()}; ema60=${ema60.compute()}; " +
          s"ema200=${ema200.compute()}; tr=${trFunction.tr(closeSlice.length - 1)}; " +
          s"dm1Plus=${dmFunction.dmPlus(closeSlice.length - 1)}; " +
          s"dm1Minus=${dmFunction.dmMinus(closeSlice.length - 1)}; " +
          s"tr14=${diMain.tr(closeSlice.length - 1)}; " +
          s"di14Plus=${diMain.diPlus(closeSlice.length - 1)}; " +
          s"di14Minus=${diMain.diMinus(closeSlice.length - 1)}; " +
          s"dx=${dxFunction.dx(closeSlice.length - 1)}; " +
          s"adx=${adxFunction.adx(closeSlice.length - 1)}")
      }

      stay()
  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }

  initialize()
}
