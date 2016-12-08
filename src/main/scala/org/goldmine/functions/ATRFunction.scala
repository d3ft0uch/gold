package org.goldmine.functions

import org.goldmine.indicator.Factor
import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

case class ATRResult(atrHour: Double, atrLastValue: Double, stopLoss: Double, pipTarget: Double)

class ATRFunction(high: Array[Double], low: Array[Double], close: Array[Double], lastATR: Double, periodDi: Factor) {

  val Coef1: Double = 1.5
  val Coef2: Double = 1.0
  val rollingProfit = 0
  
  val trueValue = ArrayBuffer.fill(close.length)(0.0)
  val atr = ArrayBuffer.fill(close.length)(0.0)

  def ATR(idx: Int) = {

    for (i <- 0 until close.length) {
      trueValue(i) =
        if (i == 0)
          Null.Double
        else
          math.max(math.max(high(i) - low(i), math.abs(high(i) - close(i - 1))), math.abs(low(i) - close(i - 1)))

      val maForTrFunction = new MAFunction(trueValue.toArray, periodDi)
      atr(i) = maForTrFunction.ma(i) * 100f

    }

    val atrLastHourValue = if (atr(idx) > lastATR) atr(idx) else lastATR
    val stopLossFactor = Coef1
    val calculatedStopLoss = stopLossFactor * math.pow(10, 2) * atrLastHourValue
    val pipTarget = calculatedStopLoss / (stopLossFactor * Coef2)
   // val orderLot = math.abs(rollingProfit) / (pipTarget * )

    ATRResult(atr(idx), atrLastHourValue, calculatedStopLoss, pipTarget)
  }
}

