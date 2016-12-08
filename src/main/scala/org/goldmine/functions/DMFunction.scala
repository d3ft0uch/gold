package org.goldmine.functions

import org.goldmine.timeseries.Null

import scala.collection.mutable.ArrayBuffer

class DMFunction(H: Array[Double], L: Array[Double], C: Array[Double]) {

  val _dmPlus = ArrayBuffer.fill(C.length)(0.0)
  val _dmMinus = ArrayBuffer.fill(C.length)(0.0)
  var alreadyComputed = false

  protected def computeSpot(i: Int): Unit = {
    if (i == 0) {

      _dmPlus(i) = Null.Double
      _dmMinus(i) = Null.Double

    } else {

      _dmPlus(i) = if(H(i) - H(i - 1)>L(i - 1) - L(i)) math.max(H(i) - H(i - 1), 0f) else 0f
      _dmMinus(i) = if(L(i - 1) - L(i)>H(i) - H(i - 1)) math.max(L(i - 1) - L(i), 0f) else 0f

      /*if (H(i) > H(i - 1) && L(i) > L(i - 1)) {
        _dmPlus(i) = H(i) - H(i - 1)
        _dmMinus(i) = 0f
      } else if (H(i) < H(i - 1) && L(i) < L(i - 1)) {
        _dmPlus(i) = 0f
        _dmMinus(i) = L(i - 1) - L(i)
      } else if (H(i) > H(i - 1) && L(i) < L(i - 1)) {
        if (H(i) - H(i - 1) > L(i - 1) - L(i)) {
          _dmPlus(i) = H(i) - H(i - 1)
          _dmMinus(i) = 0f
        } else {
          _dmPlus(i) = 0f
          _dmMinus(i) = L(i - 1) - L(i)
        }
      } else if (H(i) < H(i - 1) && L(i) > L(i - 1)) {
        _dmPlus(i) = 0f
        _dmMinus(i) = 0f
      } else if (H(i) == H(i - 1) && L(i) == L(i - 1)) {
        _dmPlus(i) = 0f
        _dmMinus(i) = 0f
      } else if (L(i) > H(i - 1)) {
        _dmPlus(i) = H(i) - H(i)
        _dmMinus(i) = 0f
      } else if (H(i) < L(i - 1)) {
        _dmPlus(i) = 0f
        _dmMinus(i) = L(i - 1) - L(i)
      } else {
        _dmPlus(i) = 0f
        _dmMinus(i) = 0f
      }*/

    }
  }

  def dmPlus(idx: Int): Double = {
    var i = 0

    if (!alreadyComputed) {
      while (i < C.length) {
        computeSpot(i)
        i += 1
      }
    }

    alreadyComputed = true

    _dmPlus(idx)
  }

  def dmMinus(idx: Int): Double = {
    var i = 0

    if (!alreadyComputed) {
      while (i < C.length) {
        computeSpot(i)
        i += 1
      }
    }

    alreadyComputed = true

    _dmMinus(idx)
  }
}

