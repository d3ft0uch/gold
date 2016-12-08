
package org.goldmine.math

import org.goldmine.collection.ArrayList
import org.goldmine.timeseries.Null

object PatchedStatsFunctions {

  def ema(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    emaOnDoubles(idx, values, period, prev)
  }

  def ema1(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    var value = values(idx)
    if (Null.is(value))
      value = 0.0

    if (Null.is(prev)) {
      0F
    } else {
      if (idx > period-1) {
        val k = 2.0F / (1.0F + period.toFloat)
        value.toFloat * k.toFloat + prev.toFloat * (1.0F - k.toFloat)
      } else if (idx == period-1) {
        StatsFunctions.ma(values, 0, idx)
      } else {
        0D
      }
    }
  }

  def emaOnDoubles(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    var value = values(idx)
    if (Null.is(value))
      value = 0.0D

    if (Null.is(prev)) {
      0D
    } else {
      if (idx > period-1) {
        val k = 2.0D / (1.0D + period)
        value * k + prev * (1.0D - k)
      } else if (idx == period-1) {
        StatsFunctions.ma(values, 0, idx)
      } else {
        0D
      }
    }
  }

  def emaOnBigNumber(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    var value = values(idx)
    if (Null.is(value))
      value = 0.0D

    if (Null.is(prev)) {
      0D
    } else {
      if (idx > period-1) {
        val k = BigDecimal(2) / (BigDecimal(1) + BigDecimal(period))
        (BigDecimal(value) * k + BigDecimal(prev) * (BigDecimal(1) - k)).toDouble
      } else if (idx == period-1) {
        StatsFunctions.ma(values, 0, idx)
      } else {
        0D
      }
    }
  }

  def emaOnBigNumber2(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    var value = values(idx)
    if (Null.is(value))
      value = 0.0D

    if (Null.is(prev)) {
      0D
    } else {
      if (idx > period-1) {
        val k = BigDecimal(2) / (BigDecimal(1) + BigDecimal(period))
        (BigDecimal(prev) + k*(BigDecimal(value) - BigDecimal(prev))).toDouble
      } else if (idx == period-1) {
        StatsFunctions.ma(values, 0, idx)
      } else {
        0D
      }
    }
  }

  def avg(x: Double, y: Double, precision: Int) = {
    ((BigDecimal(x) + BigDecimal(y)) / 2).setScale(precision, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

}

