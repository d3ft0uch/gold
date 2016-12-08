package org.akkatrading.live.util

/**
  * Created by kudr on 26.02.16.
  */

object Side extends Enumeration {
  implicit def convert(value: Value) = value.asInstanceOf[SideVal]
  implicit def fromStr(s: String): SideVal=Side.withName(s).asInstanceOf[SideVal]
  implicit def toStr(g: SideVal): String=g.toString
  case class SideVal(desc: String) extends super.Val
  val buy=SideVal("buy")
  val sell=SideVal("sell")
}