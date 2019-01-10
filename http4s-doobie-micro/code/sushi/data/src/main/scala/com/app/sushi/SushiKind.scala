package com.app.sushi

import cats.Show
import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class SushiKind(name: String, setSize: Int, price: Long)

object SushiKind {
  implicit val encoder: Encoder[SushiKind] = deriveEncoder
  implicit val decoder: Decoder[SushiKind] = deriveDecoder

  implicit val show: Show[SushiKind] = s =>
    show"SushiKind(name = ${s.name}, setSize = ${s.setSize}, price = ${s.price})"
}
