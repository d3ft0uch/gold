package org.akkatrading.live.util

/**
  * Created by kudr on 26.02.16.
  */

object OrderType extends Enumeration {
  implicit def convert(value: Value) = value.asInstanceOf[OrderTypeVal]
  implicit def fromStr(s: String): OrderTypeVal=Side.withName(s).asInstanceOf[OrderTypeVal]
  implicit def toStr(g: OrderTypeVal): String=g.toString
  case class OrderTypeVal(desc: String) extends super.Val
  val limit=OrderTypeVal("limit")
  val stop=OrderTypeVal("stop")
  val marketIfTouched=OrderTypeVal("marketIfTouched")
  val market=OrderTypeVal("market")
}