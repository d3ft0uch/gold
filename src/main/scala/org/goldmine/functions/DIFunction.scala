package org.goldmine.functions

import org.goldmine.indicator.Factor
import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

class DIFunction(H: Array[Double], L: Array[Double], C: Array[Double], period: Factor) {

  val _dmPlus = ArrayBuffer.fill(C.length)(0.0)
  val _dmMinus = ArrayBuffer.fill(C.length)(0.0)
  val _tr = ArrayBuffer.fill(C.length)(0.0)

  val _diPlus = ArrayBuffer.fill(C.length)(0.0)
  val _diMinus = ArrayBuffer.fill(C.length)(0.0)

  protected def computeSpot(i: Int): Unit = {
    val dmFunction = new DMFunction(H, L, C)
    val trFunction = new TRFunction(H, L, C)

    _dmPlus(i) = dmFunction.dmPlus(i)
    _dmMinus(i) = dmFunction.dmMinus(i)
    _tr(i) = trFunction.tr(i)

    if (i < period.value - 1) {

      _diPlus(i) = Null.Double
      _diMinus(i) = Null.Double

    } else {

      val maForDmPlusFunction = new MAFunction(_dmPlus.toArray, period)
      val maForDmMinusFunction = new MAFunction(_dmMinus.toArray, period)
      val maForTrFunction = new MAFunction(_tr.toArray, period)

      val dmPlus_ma = maForDmPlusFunction.ma(i)
      val dmMinus_ma = maForDmMinusFunction.ma(i)
      val tr_ma = maForTrFunction.ma(i)

      val diPlus_i = if (tr_ma == 0) 0f else dmPlus_ma / tr_ma * 100f
      val diMinus_i = if (tr_ma == 0) 0f else dmMinus_ma / tr_ma * 100f

      _diPlus(i) = diPlus_i
      _diMinus(i) = diMinus_i

    }
  }

  def diPlus(idx: Int): Double = {
    var i = 0

    while (i < C.length) {
      computeSpot(i)
      i += 1
    }

    _diPlus(idx)
  }

  def diMinus(idx: Int): Double = {
    var i = 0

    while (i < C.length) {
      computeSpot(i)
      i += 1
    }

    _diMinus(idx)
  }

  def tr(idx: Int): Double = {
    var i = 0

    while (i < C.length) {
      computeSpot(i)
      i += 1
    }

    _tr(idx)
  }
}

