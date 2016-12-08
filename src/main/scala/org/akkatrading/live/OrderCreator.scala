package org.akkatrading.live

import java.net.URLEncoder
import java.time.ZonedDateTime
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import org.akkatrading.live.OrderCreator.{CreateOrderConfirmation, CreateOrderRequest}
import org.akkatrading.live.util.DateUtils._
import org.akkatrading.live.util.NumberUtils._
import spray.client.pipelining._
import spray.http._
import spray.json.DefaultJsonProtocol
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.akkatrading.live.OrderCreator.CreateOrderFailure

object OrderCreator {

  def props(connector: ActorRef): Props = Props(new OrderCreator(connector))

  case class CreateOrderRequest(instrument: String, units: Int, side: String, `type`: String, expiry: Option[ZonedDateTime], price: Option[Double], stopLoss: Option[Double], takeProfit: Option[Double])

  case class TradeOpened(id: Long, units: Int, side: String, takeProfit: Double, stopLoss: Double, trailingStop: Double)

  case class OrderOpened(id: Long, units: Int, side: String, expiry: ZonedDateTime, upperBound: Double, lowerBound: Double, takeProfit: Double,
                         stopLoss: Double, trailingStop: Double)

  case class CreateOrderConfirmation(instrument: String, time: ZonedDateTime, price: Double, tradeOpened: TradeOpened)
  
  case class CreateOrderFailure()
  

  object CreateOrderJsonProtocol extends DefaultJsonProtocol {
    implicit val orderOpenedFormat = jsonFormat6(TradeOpened)
    implicit val createOrderConfirmationFormat = jsonFormat4(CreateOrderConfirmation)
  }

}

class OrderCreator(connector: ActorRef) extends Actor with ActorLogging with AuthInfo {

  import context.dispatcher

  implicit val timeout = Timeout(5 seconds)

  import org.akkatrading.live.OrderCreator.CreateOrderJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  val logResponse: HttpResponse => HttpResponse = {
    r => log.info("CreateOrderConfirmationResponse: {}", r)
    r
  }

  val orderCreatePipeline = (addCredentials(OAuth2BearerToken(authToken))
    ~> sendReceive(connector)
    ~> logResponse
    ~> unmarshal[CreateOrderConfirmation]
    )

  override def receive = {
    case request: CreateOrderRequest => handleRequest(sender(), request)
  }

  def handleRequest(sender: ActorRef, orderRequest: CreateOrderRequest) = {
    val s = sender
    log.info("Trying to send order: {}", orderRequest)
    val request =
      HttpRequest(
        method = HttpMethods.POST,
        uri = s"/v1/accounts/$accountId/orders",
        entity =
          HttpEntity(
            ContentType(MediaTypes.`application/x-www-form-urlencoded`),
            s"instrument=${orderRequest.instrument}" +
              s"&units=${orderRequest.units}" +
              s"&side=${orderRequest.side}" +
              s"&type=${orderRequest.`type`}" +
              orderRequest.expiry.map(ex => s"&expiry=${URLEncoder.encode(dateTimeFormatter.format(ex), "UTF-8")}").getOrElse("") +
              orderRequest.price.map(pr => s"&price=${decimalFormatter.format(pr)}").getOrElse("") +
              orderRequest.stopLoss.map(sl => s"&stopLoss=${decimalFormatter.format(sl)}").getOrElse("") +
              orderRequest.takeProfit.map(tp => s"&takeProfit=${decimalFormatter.format(tp)}").getOrElse("")
          )
      )
    orderCreatePipeline(request) onComplete {
      case Success(conf: CreateOrderConfirmation) =>
        log.info("Order opened: {}", conf)
        s ! conf

      case Success(somethingUnexpected) =>
        log.warning("The Oanda API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
       s ! new CreateOrderFailure()
      case Failure(error) =>
        log.error(error, "Couldn't place order")
        s ! new CreateOrderFailure()
    }
  }
}