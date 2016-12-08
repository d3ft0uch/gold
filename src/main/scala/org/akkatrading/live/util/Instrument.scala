package org.akkatrading.live.util

import org.akkatrading.live.util.InstrumentEnum.InstrumentVal
import org.akkatrading.live.util.TimeFrameEnum.TimeFrameVal

/**
  * Created by kudr on 26.02.16.
  */

object InstrumentEnum extends Enumeration {
  implicit def convert(value: Value) = value.asInstanceOf[InstrumentVal]
  implicit def fromStr(s: String): InstrumentVal=InstrumentEnum.withName(s).asInstanceOf[InstrumentVal]
  implicit def toStr(g: InstrumentVal): String=g.toString
  case class InstrumentVal(desc: String) extends super.Val
  val GBP_USD=InstrumentVal("GBP_USD")
  val EUR_USD=InstrumentVal("EUR_USD")
}

class Instrument(instrument: InstrumentVal, timeFrames: Set[TimeFrameVal]) {

  val timeFramesMap = timeFrames.foldLeft(Map[TimeFrameVal, TimeFrame]())((m, tf) => m + (tf -> new TimeFrame(tf)))

  def getTimeFrame(timeFrame: TimeFrameVal) = {
    timeFramesMap.get(timeFrame)
  }

}
