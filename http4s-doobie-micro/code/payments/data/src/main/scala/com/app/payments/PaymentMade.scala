package com.app.payments

import io.circe.{Decoder, Encoder}
import scalaz.deriving

@deriving(Encoder, Decoder)
case class PaymentMade(value: Long) extends AnyVal
