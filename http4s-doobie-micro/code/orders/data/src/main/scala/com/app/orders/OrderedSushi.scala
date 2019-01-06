package com.app.orders

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class OrderedSushi(amount: Int, kind: String, price: Long)

object OrderedSushi {
  implicit val encoder: Encoder[OrderedSushi] = deriveEncoder
  implicit val decoder: Decoder[OrderedSushi] = deriveDecoder
}
