package org.akkatrading.live.util

import java.time.ZonedDateTime

import org.akkatrading.live.CandleFetcher.{BulkPriceUpdate, Candle, PriceUpdate}
import org.akkatrading.live.PriceListener.Tick
import org.akkatrading.live.util.InstrumentEnum.InstrumentVal
import org.akkatrading.live.util.TimeFrameEnum.TimeFrameVal
import org.goldmine.math.PatchedStatsFunctions._

import scala.collection.parallel.mutable

/**
 * Created by andrey on 19.10.15.
 */
object Quotes {

  val quotesMap = mutable.ParTrieMap[InstrumentVal, Instrument]()

  def init(instruments: Set[InstrumentVal], timeFrames: Set[TimeFrameVal]) = {
    for (instrument <- instruments) {
      quotesMap += (instrument -> new Instrument(instrument, timeFrames))
    }
  }

  val precision = 5


  case class FullCandle(time: ZonedDateTime, open: Double, openBid: Double, openAsk: Double, high: Double, highBid: Double, highAsk: Double, low: Double, lowBid: Double, lowAsk: Double, close: Double, closeBid: Double, closeAsk: Double, volume: Int, complete: Boolean) {

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
          open == that.open &&
          openBid == that.openBid &&
          openAsk == that.openAsk &&
          high == that.high &&
          highBid == that.highBid &&
          highAsk == that.highAsk &&
          low == that.low &&
          lowBid == that.lowBid &&
          lowAsk == that.lowAsk &&
          close == that.close &&
          closeBid == that.closeBid &&
          closeAsk == that.closeAsk
      case _ => false
    }

    def this(c: Candle) = {
      this(c.time,
        avg(c.openBid, c.openAsk, precision),
        c.openBid, c.openAsk, math.max(c.highBid, c.highAsk), c.highBid, c.highAsk,
        math.min(c.lowBid, c.lowAsk), c.lowBid, c.lowAsk,
        avg(c.closeBid, c.closeAsk, precision),
        c.closeBid, c.closeAsk, c.volume, c.complete)
    }

  }

  object FullCandle {

    def apply(c: Candle) = {
      new FullCandle(c.time,
        avg(c.openBid, c.openAsk, precision),
        c.openBid, c.openAsk, math.max(c.highBid, c.highAsk), c.highBid, c.highAsk,
        math.min(c.lowBid, c.lowAsk), c.lowBid, c.lowAsk,
        avg(c.closeBid, c.closeAsk, precision),
        c.closeBid, c.closeAsk, c.volume, c.complete)
    }

    def update(c: FullCandle, completed: Boolean) = {
      new FullCandle(c.time, c.open, c.openBid, c.openAsk, math.max(c.highBid, c.highAsk), c.highBid, c.highAsk, math.min(c.lowBid, c.lowAsk), c.lowBid, c.lowAsk, c.close, c.closeBid, c.closeAsk, c.volume, completed)
    }
  }

  def getQuotes(instrument: InstrumentVal, timeFrame: TimeFrameVal) = {
    quotesMap.get(instrument).get.getTimeFrame(timeFrame).get
  }

  def handleBulkPriceUpdate(update: BulkPriceUpdate): Boolean = {
    getQuotes(update.instrument, update.granularity).handleBulkPriceUpdate(update)
  }

  def handlePriceUpdate(update: PriceUpdate) = {
    getQuotes(update.instrument, update.granularity).handlePriceUpdate(update)
  }

  def handleTick(tick: Tick) = {
    getQuotes(InstrumentEnum.fromStr(tick.instrument), TimeFrameEnum.M1).handleTick(tick)
  }

  def enoughCandles(instrument: InstrumentVal, timeFrame: TimeFrameVal, n: Int): Boolean = {
    getQuotes(instrument, timeFrame).enoughCandles(n)
  }

}
