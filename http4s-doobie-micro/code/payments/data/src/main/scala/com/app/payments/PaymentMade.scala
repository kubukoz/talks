package com.app.payments

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._

case class PaymentMade(value: Long) extends AnyVal

object PaymentMade {
  implicit val encoder: Encoder[PaymentMade] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[PaymentMade] = deriveUnwrappedDecoder
}
