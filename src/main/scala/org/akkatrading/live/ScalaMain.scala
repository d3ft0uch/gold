package org.akkatrading.live

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.akkatrading.live.EventListener.SubscribeToEvents
import org.akkatrading.live.PriceListener.SubscribeToRates
import spray.can.Http
import spray.can.Http.HostConnectorInfo

import scala.concurrent.duration._

object ScalaMain extends App {
//scala -classpath akka-trading-assembly-1.0.jar org.akkatrading.live.ScalaMain
  implicit val timeout = Timeout(5 seconds)
  implicit val system = ActorSystem("oanda-client")

  import org.akkatrading.live.ScalaMain.system.dispatcher

  val streamingConnectorFuture = for {
    HostConnectorInfo(hostConnector, _) <- IO(Http) ? Http.HostConnectorSetup("stream-fxpractice.oanda.com", port = 443, sslEncryption = true)
  } yield hostConnector

  val restConnectorFuture = for {
    HostConnectorInfo(hostConnector, _) <- IO(Http) ? Http.HostConnectorSetup("api-fxpractice.oanda.com", port = 443, sslEncryption = true)
  } yield hostConnector

  restConnectorFuture onSuccess {
    case restConnector =>
      val orderManager = system.actorOf(StrategyFSM.props(restConnector), "orderManager")
      streamingConnectorFuture onSuccess {
        case streamingConnector =>
          val eventListener = system.actorOf(EventListener.props(streamingConnector, orderManager), "eventListener")
          eventListener ! SubscribeToEvents
          val priceListener = system.actorOf(PriceListener.props(streamingConnector, orderManager), "priceListener")
          priceListener ! SubscribeToRates()
      }
  }
}
