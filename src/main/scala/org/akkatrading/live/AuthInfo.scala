package org.akkatrading.live

import com.typesafe.config.ConfigFactory
import org.akkatrading.live.util.InstrumentEnum.InstrumentVal
import org.akkatrading.live.util.TimeFrameEnum.TimeFrameVal
import org.akkatrading.live.util.{TimeFrameEnum, InstrumentEnum}
import scala.collection.JavaConversions._

trait AuthInfo {

  private val config = ConfigFactory.load()
  private val goldMineConfig = config.getConfig("goldmine")

  val accountId = goldMineConfig.getString("accountId") // your real account id
  val authToken = goldMineConfig.getString("authToken") // your real Oanda REST API access token
  val instruments = goldMineConfig.getStringList("instruments").map(InstrumentEnum.fromStr).toSet
  val timeFrames = goldMineConfig.getStringList("timeFrames").map(TimeFrameEnum.fromStr).toSet

}
