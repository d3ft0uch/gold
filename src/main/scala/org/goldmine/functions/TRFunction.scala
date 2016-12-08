package org.goldmine.functions

import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

class TRFunction(H: Array[Double], L: Array[Double], C: Array[Double]) {

  val _tr = ArrayBuffer.fill(C.length)(0.0)

  protected def computeSpot(i: Int): Unit = {
    if (i == 0) {

      _tr(i) = Null.Double

    } else {

      val tr_tmp = math.max(H(i) - L(i), math.abs(H(i) - C(i - 1)))
      _tr(i) = math.max(tr_tmp, math.abs(L(i) - C(i - 1)))

    }
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

