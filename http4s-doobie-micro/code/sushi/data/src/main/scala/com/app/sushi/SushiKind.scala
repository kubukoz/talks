package com.app.sushi

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class SushiKind(name: String, setSize: Int, price: Long)

object SushiKind {
  implicit val encoder: Encoder[SushiKind] = deriveEncoder
  implicit val decoder: Decoder[SushiKind] = deriveDecoder
}
