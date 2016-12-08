package org.akkatrading.live.util

import java.time.temporal.ChronoUnit

import org.akkatrading.live.CandleFetcher.{PriceUpdate, BulkPriceUpdate}
import org.akkatrading.live.PriceListener.Tick
import org.akkatrading.live.util.Quotes.FullCandle
import org.akkatrading.live.util.TimeFrameEnum.TimeFrameVal
import org.goldmine.math.PatchedStatsFunctions._
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created by kudr on 26.02.16.
  */

object TimeFrameEnum extends Enumeration {
  implicit def convert(value: Value) = value.asInstanceOf[TimeFrameVal]
  implicit def fromStr(s: String): TimeFrameVal=TimeFrameEnum.withName(s).asInstanceOf[TimeFrameVal]
  case class TimeFrameVal(desc: String, minutes: Int) extends super.Val
  val M1=TimeFrameVal("M1", 1)
  val M5=TimeFrameVal("M5", 5)
}

class TimeFrame(timeFrame: TimeFrameVal) {

  val logger = LoggerFactory.getLogger(this.getClass)

  val quotes = new ArrayBuffer[FullCandle]()

  def handleBulkPriceUpdate(update: BulkPriceUpdate): Boolean = {

    logger.info(s"Starting bulk price update for ${update.instrument} ${update.granularity}") //Quotes: ${update.candles.mkString("\n")}")

    for (candle <- update.candles) {
      var found = false
      var i = quotes.length-1
      while (!found && i >= 0) {
        val candleComapared = candle.time.compareTo(quotes(i).time)
        if (candleComapared > 0) {
          found = true
          quotes.insert(i+1, FullCandle(candle))
        } else if (candleComapared == 0) {
          found = true
          quotes(i) = FullCandle(candle)
        }
        i += -1
      }
      if (!found && (quotes.isEmpty || candle.time.compareTo(quotes(0).time) < 0))
        FullCandle(candle) +=: quotes
    }

    logger.info(s"Finished bulk price update for ${update.instrument} ${update.granularity}") //. Quotes: ${quotes.mkString("\n")}")

    checkConsistency()
  }

  def checkConsistency(): Boolean = {
    var consistent = true
    var i = 0
    for (i <- 1 until quotes.length) {
      if (quotes(i).time.minusMinutes(timeFrame.minutes) != quotes(i-1).time) {
        consistent = false
        logger.error("Inconsistent candles found: {}, previous: {}", quotes(i), quotes(i-1): Any)
      }
    }
    if (consistent)
      logger.debug("Consistency successfully checked")
    consistent
  }

  def getSlice(period: Int) = {
    quotes.slice(quotes.length - period*2, quotes.length)
  }

  def lastCandle: FullCandle = {
    quotes.last
  }

  def newCandleFromTick(tick: Tick): FullCandle = {
    val average = avg(tick.bid, tick.ask, Quotes.precision)
    FullCandle(tick.time.truncatedTo(ChronoUnit.MINUTES), average, tick.bid, tick.ask, math.max(tick.bid, tick.ask), tick.bid, tick.ask, math.min(tick.bid, tick.ask), tick.bid, tick.ask, average, tick.bid, tick.ask, 1, false)
  }

  def handleTick(tick: Tick) = {
    //TODO: think about thread safety. I think there must be locks on quotes
    if (quotes.isEmpty) {
      //Making the first candle
      quotes += newCandleFromTick(tick)
    } else {
      val freshCandle = quotes.last
      if (freshCandle.time == tick.time.truncatedTo(ChronoUnit.MINUTES)) {
        //Updating current candle
        quotes(quotes.length - 1) = FullCandle(
          freshCandle.time,
          freshCandle.open,
          freshCandle.openBid,
          freshCandle.openAsk,
          Array(freshCandle.highBid, tick.bid, freshCandle.highAsk).max,
          math.max(freshCandle.highBid, tick.bid),
          math.max(freshCandle.highAsk, tick.ask),
          Array(freshCandle.lowBid, tick.bid, freshCandle.lowAsk).min,
          math.min(freshCandle.lowBid, tick.bid),
          math.min(freshCandle.lowAsk, tick.ask),
          avg(tick.bid, tick.ask, Quotes.precision),
          tick.bid,
          tick.ask,
          freshCandle.volume + 1,
          freshCandle.complete
        )
      } else {
        if (tick.time.getMinute == freshCandle.time.getMinute + timeFrame.minutes || tick.time.getMinute == 0 && freshCandle.time.getMinute == 59) {
          //Making new candle and updating previous
          quotes(quotes.length - 1) = FullCandle.update(quotes.last, completed = true)
          quotes += newCandleFromTick(tick)
        } else {
          //TODO: tell to orderManager about problem and try to download missing candles
          logger.error("Inconsistent candles!!! Download missing candles! TickTime: {}; FreshCandleTime: {}", tick.time.getMinute, freshCandle.time.getMinute)
        }
      }
    }

    //logger.debug("Stored quotes: " + quotes.mkString(","))
  }

  //Makes simple checks of candles integrity up to 2 candles deep.
  def handlePriceUpdate(update: PriceUpdate) = {

    if (quotes.isEmpty) {
      //Making the first candles
      quotes += FullCandle(update.previous)
      quotes += FullCandle(update.current)
    } else {
      if (update.current.time.compareTo(quotes.last.time) > 0) {
        // If CandleFetcher have the fresher candles then we collected from stream. So we're having consistency problems?
        logger.error("Inconsistent candles!!! Download missing candles! CurrentCandleTime: {}, quotes.last.time={}", update.current.time, quotes.last.time: Any)
        tryToFixConsistency(update)
      } else {
        // PriceFetcher provides fresh ticks. Good. We should match our home-made candles from stream with Oanda historical candles from CandleFetcher.
        if (update.current.time == quotes.last.time) {
          if ( quotes.length > 1) {
            if (update.previous.time == quotes(quotes.length - 2).time) {

              //match previous candle
              if (!update.previous.equalsPrices(quotes(quotes.length - 2))) {
                logger.debug("Inconsistent candles!!! Previous CandleFetcher candle {} doesn't match PriceListener candle {}", update.previous, quotes(quotes.length - 2):Any)
              }

              quotes(quotes.length - 2) = new FullCandle(update.previous)

            } else {
              logger.error("Inconsistent candles!!! Download missing candles! update.previous.time=%d, quotes(quotes.length - 2)=%d", update.previous.time, quotes(quotes.length - 2): Any)
              tryToFixConsistency(update)
            }
          } else {
            //We have only 1 candle. Adding previous candle to quotes
            FullCandle(update.previous) +=: quotes
          }
        }
      }
    }
  }

  def tryToFixConsistency(update: PriceUpdate) = {
    if (update.previous.time == quotes.last.time) {
      //There wasn't any ticks for 500 ms. Simple to fix.
      logger.debug("Inconsistent candles trying to fix...")
      quotes += FullCandle(update.current)
      logger.debug("Inconsistent candles successfully fixed.")
    } else {
      logger.debug("Inconsistent candles cannot be quickly fixed. We need to download missing candles!!!")
    }
  }

  def enoughCandles(n: Int): Boolean = {
    quotes.length >= n
  }
}
