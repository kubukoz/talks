package com.app.sushi

import cats.Show
import io.circe.{Decoder, Encoder}
import scalaz.deriving
import cats.implicits._

@deriving(Encoder, Decoder, Show)
case class SushiKind(name: String, setSize: Int, price: Long)
