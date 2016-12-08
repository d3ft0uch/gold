package org.goldmine.functions

import org.goldmine.indicator.Factor
import org.goldmine.math.StatsFunctions
import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

class MAFunction(baseVar: Array[Double], period: Factor) {

  final protected def ima(idx: Int, baseVar: Array[Double], period: Double, prev: Double): Double = {
    StatsFunctions.ima(idx, baseVar, period.toInt, prev)
  }

  val _ma = ArrayBuffer.fill(baseVar.length)(0.0)

  protected def computeSpot(i: Int): Unit = {
    if (i < period.value - 1) {

      _ma(i) = Null.Double

    } else {

      _ma(i) = ima(i, baseVar, period.value, _ma(i - 1))

    }
  }

  def ma(idx: Int): Double = {
    var i = 0

    while (i < baseVar.length) {
      computeSpot(i)
      i += 1
    }

    _ma(idx)
  }

}

