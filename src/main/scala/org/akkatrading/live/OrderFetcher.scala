package org.akkatrading.live

import java.time.ZonedDateTime
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import org.akkatrading.live.util.DateUtils._
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.akkatrading.live.OrderFetcher.FetchTradeResponse
import org.akkatrading.live.OrderFetcher.FetchTradeResponse
import org.akkatrading.live.OrderFetcher.FetchOrderRequest

object OrderFetcher {

  def props(connector: ActorRef): Props = Props(new OrderFetcher(connector))

  //count=50 by default
  //ids - An URL encoded comma (%2C) separated list of orders to retrieve. Maximum number of ids: 50.
  case class FetchOrderRequest(maxId: Option[Int]=None, count: Option[Int]=None, instrument: Option[String]=None, ids: Option[String]=None)

  

  
  case class Trade(id: Long, instrument: String, units: Int, side: String,
                         time: ZonedDateTime, price: Double, takeProfit: Double, trailingAmount: Double, stopLoss: Double, trailingStop: Double)

  case class FetchTradeResponse(trades: List[Trade])


  object FetchOrderJsonProtocol extends DefaultJsonProtocol {
    implicit val tradeFmt = jsonFormat10(Trade)
    implicit val fetchTradeFormat = jsonFormat1(FetchTradeResponse)
  }

}

class OrderFetcher(connector: ActorRef) extends Actor with ActorLogging with AuthInfo {

  import context.dispatcher
  import org.akkatrading.live.OrderFetcher.FetchOrderJsonProtocol._

  implicit val timeout = Timeout(5 seconds)

//  context.system.scheduler.schedule(0 milliseconds, 5000 milliseconds, self, FetchOrderRequest(None, None, None, None))

  val pipeline = addCredentials(OAuth2BearerToken(authToken)) ~> sendReceive(connector) ~> unmarshal[FetchTradeResponse]
  
  

  def receive = {
    case FetchOrderRequest(maxId, count, instrument, ids) =>
      val s = sender
      log.info("message sent from s '{}' '{}'", s, s.path)
      pipeline(Get(s"/v1/accounts/$accountId/trades")) onComplete {
        case Success(tradesResponse: FetchTradeResponse) =>
          log.info("Orders Fetched: {}", tradesResponse)
          s ! new FetchTradeResponse(tradesResponse.trades.sortBy { _.id })

        case Success(somethingUnexpected) =>
          log.warning("The Oanda API call was successful but returned something unexpected: '{}'.", somethingUnexpected)

        case Failure(error) =>
          log.error(error, "Couldn't fetch orders")
      }
  }
}

