package com.app.payments
import cats.Show
import eu.timepit.refined.types.numeric.PosLong
import eu.timepit.refined.cats.refTypeShow
import cats.implicits._
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import scalaz.deriving

@deriving(Encoder, Decoder, Show)
case class PaymentRequest(amount: PosLong)
