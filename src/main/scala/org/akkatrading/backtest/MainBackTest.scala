package org.akkatrading.backtest

/**
 * Created by andrey on 06.10.15.
 */
object MainBackTest extends App{
  akka.Main.main(Array(classOf[Backtest].getName))
}
