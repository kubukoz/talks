package com.orders

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class OrderedSushi(amount: Int, kind: String, price: Long)

object OrderedSushi {
  implicit val encoder: Encoder[OrderedSushi] = deriveEncoder
  implicit val decoder: Decoder[OrderedSushi] = deriveDecoder
}
