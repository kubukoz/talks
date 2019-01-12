package com.app.orders.config

import com.app.orders.payments.PaymentsClient
import com.app.orders.sushi.SushiClient
import com.app.orders.util.AskFunctions
import pureconfig.ConfigReader
import scalaz.deriving

@deriving(ConfigReader)
case class OrderServiceConfiguration(sushiService: SushiClient.Configuration,
                                     paymentsService: PaymentsClient.Configuration)

object OrderServiceConfiguration extends AskFunctions[OrderServiceConfiguration]
