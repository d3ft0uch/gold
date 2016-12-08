package org.akkatrading.backtest

import java.time.{ZoneId, ZonedDateTime, LocalDateTime}
import java.time.format.DateTimeFormatter

import akka.actor.Actor
import org.akkatrading.backtest.PriceDataCsvReader.{PriceTicks, ReadTicksFromCsv}
import org.akkatrading.backtest.model.CandleWithIndicators

import scala.io.Source

object PriceDataCsvReader {

  case class ReadTicksFromCsv(fileName: String)

  case class PriceTicks(priceTicks: List[CandleWithIndicators])

}

class PriceDataCsvReader extends Actor {

  val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss") //"dd.MM.uuuu HH:mm:ss"

  def receive = {
    case ReadTicksFromCsv(fileName: String) =>
      val ticks = Source.fromFile(fileName).getLines().map {
        line =>
          val tokens = line.split(";")
            CandleWithIndicators(LocalDateTime.parse(tokens(0), dateTimeFormatter).atZone(ZoneId.systemDefault()),
              tokens(1).toDouble, tokens(1).toDouble, tokens(1).toDouble,
              tokens(4).toDouble, tokens(4).toDouble, tokens(4).toDouble,
              tokens(7).toDouble, tokens(7).toDouble, tokens(7).toDouble,
              tokens(10).toDouble, tokens(10).toDouble, tokens(10).toDouble,
              1, true,
              tokens(13).toDouble, tokens(14).toDouble, tokens(15).toDouble,   //EMA 5, EMA 60, EMA 200
              tokens(16).toDouble, tokens(17).toDouble, //ADX +DI, ADX +DI Prev
              tokens(18).toDouble, tokens(19).toDouble,  //ADX -DI, ADX -DI Prev
              tokens(20).toDouble, tokens(21).toDouble,  //ADX Main, ADX Main Prev
              tokens(22).toDouble, tokens(23).toDouble,  //ATR 1m, ATR 1m Prev
              tokens(24).toDouble, tokens(25).toDouble  //ATR 1h, ATR 1h Prev
            )
      }.toList
      sender() ! PriceTicks(ticks)
  }
}
