package com.app.payments

import eu.timepit.refined.types.numeric.PosLong
import io.circe.{Decoder, Encoder}
import scalaz.deriving
import io.circe.refined._

@deriving(Encoder, Decoder)
case class PaymentMade(amount: PosLong)
