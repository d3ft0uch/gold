package org.goldmine.functions

import org.goldmine.indicator.Factor
import org.goldmine.math.PatchedStatsFunctions

import scala.collection.mutable.ArrayBuffer

class EMAFunction(baseVar: Array[Double], period: Factor) {

  final protected def ema(idx: Int, var1: Array[Double], period: Double, prev: Double): Double = {
    PatchedStatsFunctions.ema(idx, var1, period.toInt, prev)
  }

  val _ema = ArrayBuffer.fill(baseVar.length)(0.0)

  protected def computeSpot(i: Int): Unit = {
    if (i == 0) {

      if (period.value > 1)
        _ema(i) = 0D
      else
        baseVar(0)

    } else {

      _ema(i) = ema(i, baseVar, period.value, _ema(i - 1))

    }
  }

  def compute(): Double = {

    //TODO: reuse computedSpots

    var i = 0

    while (i < baseVar.length) {
      computeSpot(i)
      i += 1
    }

    _ema(i-1)
  }

}

