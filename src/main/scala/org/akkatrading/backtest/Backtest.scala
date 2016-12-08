package org.akkatrading.backtest

import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, Props, ReceiveTimeout}
import org.akkatrading.backtest.PriceDataCsvReader.{PriceTicks, ReadTicksFromCsv}
import org.akkatrading.backtest.model.LogMessage
import org.akkatrading.backtest.strategy.StrategyFSM

import scala.concurrent.duration._

class Backtest extends Actor with ActorLogging {

  import context._

  val formatter = new DecimalFormat("#.######")

  override def preStart() {
    val csvReader = actorOf(Props[PriceDataCsvReader], "csvReader")
    csvReader ! ReadTicksFromCsv("src/main/resources/GBPUSD_1_1454643595_candles.csv")
    //GBPJPY_1_1444752147_candles.csv
    //GBPJPY_1_10_candles.csv

    //"src/main/resources/eurusd_daily_2004_2014.csv" "GBPJPY_1_candles (1).csv"
  }

  context.setReceiveTimeout(360 second)

  def receive = {
    case PriceTicks(priceTicks) =>
      val strategyFsm = actorOf(Props[StrategyFSM], "strategyFsm")
      for (tick <- priceTicks) {
        strategyFsm ! tick
      }
    case LogMessage(d, intervalPnl, tradePnl, logMsg) =>
      println(s"${DateTimeFormatter.ofPattern("dd.MM.uuuu\tHH:mm:ss").format(d)}\t${formatter.format(intervalPnl)}\t${formatter.format(tradePnl)}\t$logMsg")
    case ReceiveTimeout =>
      stop(self)
  }
}
