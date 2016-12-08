package org.akkatrading.live

import java.time.ZonedDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import org.akkatrading.live.PriceListener.{Tick, HeartbeatResponse, PriceResponse, SubscribeToRates}
import org.akkatrading.live.util.DateUtils._
import spray.http._
import spray.httpx.RequestBuilding._
import spray.httpx.unmarshalling._
import spray.json._

import scala.concurrent.duration._

object PriceListener {

  def props(hostConnector: ActorRef, orderManager: ActorRef): Props = Props(new PriceListener(hostConnector, orderManager))

  case class SubscribeToRates()

  case class Tick(instrument: String, time: ZonedDateTime, bid: Double, ask: Double)

  case class PriceResponse(tick: Tick)

  case class Heartbeat(time: ZonedDateTime)

  case class HeartbeatResponse(heartbeat: Heartbeat)

  object PriceJsonProtocol extends DefaultJsonProtocol {
    implicit val tickFormat = jsonFormat4(Tick)
    implicit val priceFormat = jsonFormat1(PriceResponse)
    implicit val heartbeatFormat = jsonFormat1(Heartbeat)
    implicit val heartbeatResponseFormat = jsonFormat1(HeartbeatResponse)
  }

}

class PriceListener(hostConnector: ActorRef, orderManager: ActorRef) extends Actor with ActorLogging with AuthInfo {

  implicit val timeout: Timeout = Timeout(15.seconds)

  import org.akkatrading.live.PriceListener.PriceJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  def receive = {
    case SubscribeToRates() =>
      hostConnector ! Get(s"/v1/prices?accountId=$accountId&instruments=${instruments.mkString(",")}") ~> addCredentials(OAuth2BearerToken(authToken))
    case MessageChunk(data, _) =>
      data.asString.lines.foreach { line =>
        val entity = HttpEntity(ContentTypes.`application/json`, line)
        val res = entity.as[PriceResponse].fold(_ => entity.as[HeartbeatResponse].fold(e => e, heartbeat => heartbeat.heartbeat), price => price.tick)

        if (res.isInstanceOf[Tick]) {
          orderManager ! res
        }

        log.debug("{}", res)
      }
    case other =>
      log.info("Received {}", other)
  }
}
