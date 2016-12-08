package org.goldmine.functions

import org.goldmine.indicator.Factor
import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

class DXFunction(H: Array[Double], L: Array[Double], C: Array[Double], period: Factor) {

  val _diPlus = ArrayBuffer.fill(C.length)(0.0)
  val _diMinus = ArrayBuffer.fill(C.length)(0.0)

  val _dx = ArrayBuffer.fill(C.length)(0.0)

  protected def computeSpot(i: Int): Unit = {

    val diFunction = new DIFunction(H, L, C, period)

    if (i < period.value - 1) {

      _diPlus(i) = diFunction.diPlus(i)
      _diMinus(i) = diFunction.diMinus(i)

      _dx(i) = Null.Double

    } else {

      _diPlus(i) = diFunction.diPlus(i)
      _diMinus(i) = diFunction.diMinus(i)

      val diPlus_i = _diPlus(i)
      val diMinus_i = _diMinus(i)

      val dx_i = if (diPlus_i + diMinus_i == 0) 0f else math.abs(diPlus_i - diMinus_i) / (diPlus_i + diMinus_i) * 100f

      _dx(i) = dx_i
    }
  }

  def dx(idx: Int): Double = {
    var i = 0

    while (i < C.length) {
      computeSpot(i)
      i += 1
    }

    _dx(idx)
  }

}

