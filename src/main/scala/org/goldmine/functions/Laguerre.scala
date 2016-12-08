package org.goldmine.functions

import org.goldmine.indicator.Factor

import scala.collection.mutable.ArrayBuffer

class Laguerre(baseVar: Array[Double], gammaFactor: Factor, maFactor: Factor, maxBarsFactor: Factor) {

  val MA: Int = maFactor.value.toInt
  val gamma: Double = gammaFactor.value
  val maxBars: Int = if (maxBars > baseVar.length) maxBarsFactor.value.toInt else baseVar.length
  val arrayVariables = ArrayBuffer.fill(maxBars)(0.0D)
  val outputs = ArrayBuffer.fill(maxBars)(0.0D)

  def compute(): ArrayBuffer[Double] = {

    /*if (MaxBars>Bars) MaxBars=Bars;
    SetIndexDrawBegin(0,Bars-MaxBars);

    for(i=MaxBars-1;i>=0;i--) { sum1=0;
      L0A = L0; L1A = L1; L2A = L2; L3A = L3;
      L0 = (1 - gamma)*Close[i] + gamma*L0A;
      L1 = - gamma *L0 + L0A + gamma *L1A;
      L2 = - gamma *L1 + L1A + gamma *L2A;
      L3 = - gamma *L2 + L2A + gamma *L3A;
      CU = 0; CD = 0;
      if (L0 >= L1) CU = L0 - L1; else CD = L1 - L0;
      if (L1 >= L2) CU = CU + L1 - L2; else CD = CD + L2 - L1;
      if (L2 >= L3) CU = CU + L2 - L3; else CD = CD + L3 - L2;
      if (CU + CD != 0) LRSI = CU / (CU + CD);
      dummy[i] = LRSI;
      if (MA < 2) Buffer1[i] = dummy[i]; else { for (j=i; j < i+MA; j++) sum1 += dummy[j]; Buffer1[i] = sum1/MA; }
    }*/


    var L0, L1, L2, L3, L0A, L1A, L2A, L3A, LRSI, CU, CD, summ : Double = 0.0
    val startIndex = baseVar.length - maxBars
    val endIndex = baseVar.length - 1

    if (startIndex <= endIndex && startIndex >= 0) {
      var j = 0
      for (i <- startIndex to endIndex) {
        summ = 0.0D
        L0A = L0
        L1A = L1
        L2A = L2
        L3A = L3
        L0 = (1 - gamma) * baseVar(i) + gamma * L0A
        L1 = (-gamma) * L0 + L0A + gamma * L1A
        L2 = (-gamma) * L1 + L1A + gamma * L2A
        L3 = (-gamma) * L2 + L2A + gamma * L3A
        CU = 0.0D
        CD = 0.0D

        if (L0 >= L1) CU = L0 - L1
        else CD = L1 - L0
        if (L1 >= L2) CU = CU + L1 - L2
        else CD = CD + L2 - L1
        if (L2 >= L3) CU = CU + L2 - L3
        else CD = CD + L3 - L2
        if (CU + CD != 0.0) LRSI = CU / (CU + CD)
        arrayVariables(j) = LRSI
        if (MA < 2)
          outputs(j) = arrayVariables(j)
        else {
          if (j > 0) {
            for (z <- j - MA + 1 to j)
              summ += arrayVariables(z)
          }
          outputs(j) = summ / MA
        }
        j += 1
      }
    }
    outputs
  }

}

