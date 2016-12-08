akka-trading
============

Scala Backtesting + Live Trading Framework built on top of Akka/Spray

What is it good for?
==============================

This framework can be useful for people coming from Scala background who are making their first steps in back/live testing automated trading strategies using Oanda's REST API, which is in my opinion one of the best available retail APIs. Since this is work in progress, if you are a Scala enthusiast and are interested in automated trading, have a look and feel free to fork repo and contribute!

Usage
=====

Just clone the repo and drop your trading logic into `StrategyFSM.scala`. Don't forget to modify `AuthInfo.scala` to include your own account ID and access token for Oanda's REST API.

