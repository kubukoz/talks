package com.app.sushi

import cats.Show
import io.circe.{Decoder, Encoder}
import scalaz.deriving
import cats.implicits._
import eu.timepit.refined.cats.refTypeShow
import eu.timepit.refined.types.numeric.{PosInt, PosLong}
import io.circe.refined._

@deriving(Encoder, Decoder, Show)
case class SushiKind(name: String, setSize: PosInt, price: PosLong)
