package org.akkatrading.live

import java.time.ZonedDateTime
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import org.akkatrading.live.OrderCanceler.{CancelOrderConfirmation, CancelOrderRequest}
import org.akkatrading.live.util.DateUtils._
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.akkatrading.live.OrderCanceler.CancelOrderFailure

object OrderCanceler {

  def props(connector: ActorRef): Props = Props(new OrderCanceler(connector))

  case class CancelOrderRequest(orderId: Long, units: Int)

  case class CancelOrderConfirmation(id: Long, instrument: String, side: String, price: Double, profit: Double, time: ZonedDateTime)
  
  case class CancelOrderFailure()

  object CancelOrderJsonProtocol extends DefaultJsonProtocol {
    implicit val cancelOrderConfirmationFormat = jsonFormat6(CancelOrderConfirmation)
  }

}

class OrderCanceler(connector: ActorRef) extends Actor with ActorLogging with AuthInfo {

  import context.dispatcher
  import org.akkatrading.live.OrderCanceler.CancelOrderJsonProtocol._

  implicit val timeout = Timeout(5 seconds)

  val pipeline = addCredentials(OAuth2BearerToken(authToken)) ~> sendReceive(connector) ~> unmarshal[CancelOrderConfirmation]

  override def receive = {
    case request: CancelOrderRequest => handleRequest(sender(), request)
  }

  def handleRequest(sender: ActorRef, orderRequest: CancelOrderRequest) = {
    val s = sender
    
    pipeline(Delete(s"/v1/accounts/$accountId/trades/${orderRequest.orderId}?units=${orderRequest.units}")) onComplete {
      case Success(conf: CancelOrderConfirmation) =>
        log.info("Limit Order canceled: {}", conf)
        s ! conf

      case Success(somethingUnexpected) =>
        log.warning("The Oanda API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
        s ! new CancelOrderFailure()
      case Failure(error) =>
        log.error(error, "Couldn't cancel order")
        s ! new CancelOrderFailure()
    }
  }
}
