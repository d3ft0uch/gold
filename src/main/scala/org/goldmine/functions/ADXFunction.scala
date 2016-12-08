package org.goldmine.functions


import org.goldmine.indicator.Factor
import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

case class ADXResult(adx: Double, adxPrev: Double, plusDi: Double, plusDiPrev: Double, minusDi: Double, minusDiPrev: Double)

class ADXFunction(H: Array[Double], L: Array[Double], C: Array[Double], periodDi: Factor, periodAdx: Factor) {

  val _adx = ArrayBuffer.fill(C.length)(0.0)
  val _dx = ArrayBuffer.fill(C.length)(0.0)
  val _diPlus = ArrayBuffer.fill(C.length)(0.0)
  val _diMinus = ArrayBuffer.fill(C.length)(0.0)
  val _dmPlus = ArrayBuffer.fill(C.length)(0.0)
  val _dmMinus = ArrayBuffer.fill(C.length)(0.0)
  val _tr = ArrayBuffer.fill(C.length)(0.0)

  def adx(idx: Int): ADXResult = {

    var i = 0

    while (i < C.length) {

      if (i == 0) {
        _dmPlus(i) = Null.Double
        _dmMinus(i) = Null.Double
      } else {
        _dmPlus(i) = if (H(i) - H(i - 1) > L(i - 1) - L(i)) math.max(H(i) - H(i - 1), 0f) else 0f
        _dmMinus(i) = if (L(i - 1) - L(i) > H(i) - H(i - 1)) math.max(L(i - 1) - L(i), 0f) else 0f
      }

      if (i == 0) {
        _tr(i) = Null.Double
      } else {
        val tr_tmp = math.max(H(i) - L(i), math.abs(H(i) - C(i - 1)))
        _tr(i) = math.max(tr_tmp, math.abs(L(i) - C(i - 1)))
      }

      if (i < periodDi.value - 1) {

        _diPlus(i) = Null.Double
        _diMinus(i) = Null.Double

      } else {

        val maForDmPlusFunction = new MAFunction(_dmPlus.toArray, periodDi)
        val maForDmMinusFunction = new MAFunction(_dmMinus.toArray, periodDi)
        val maForTrFunction = new MAFunction(_tr.toArray, periodDi)

        val dmPlus_ma = maForDmPlusFunction.ma(i)
        val dmMinus_ma = maForDmMinusFunction.ma(i)
        val tr_ma = maForTrFunction.ma(i)

        val diPlus_i = if (tr_ma == 0) 0f else dmPlus_ma / tr_ma * 100f
        val diMinus_i = if (tr_ma == 0) 0f else dmMinus_ma / tr_ma * 100f

        _diPlus(i) = diPlus_i
        _diMinus(i) = diMinus_i

      }

      if (i < periodDi.value - 1) {
        _dx(i) = Null.Double
      } else {
        val diPlus_i = _diPlus(i)
        val diMinus_i = _diMinus(i)
        val dx_i = if (diPlus_i + diMinus_i == 0) 0f else math.abs(diPlus_i - diMinus_i) / (diPlus_i + diMinus_i) * 100f
        _dx(i) = dx_i
      }

      if (i < periodDi.value - 1 || i < periodAdx.value - 1) {
        _adx(i) = Null.Double
      } else {
        val maFunction = new MAFunction(_dx.toArray, periodDi)
        _adx(i) = maFunction.ma(i)
      }

      i += 1
    }

    ADXResult(_adx(idx), _adx(idx-1), _dmPlus(idx), _dmPlus(idx-1), _dmMinus(idx), _dmMinus(idx-1))
  }

}

