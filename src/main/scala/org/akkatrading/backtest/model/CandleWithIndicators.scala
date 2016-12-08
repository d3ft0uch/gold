package org.akkatrading.backtest.model

import java.time.{ZonedDateTime, LocalDateTime}

case class CandleWithIndicators(time: ZonedDateTime, open: Double, openBid: Double, openAsk: Double,
                                high: Double, highBid: Double, highAsk: Double, low: Double, lowBid: Double, lowAsk: Double,
                                close: Double, closeBid: Double, closeAsk: Double,
                                volume: Int, complete: Boolean,
                                ema5: Double, ema60: Double, ema200: Double,
                                adxPlusDi: Double, adxPlusDiPrev: Double,
                                adxMinusDi: Double, adxMinusDiPrev: Double,
                                adxMain: Double, adxMainPrev: Double,
                                atr: Double, atrPrev: Double,
                                atr1Hour: Double, atr1HourPrev: Double) {
}
