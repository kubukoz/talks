package com.app.orders

import java.util.UUID

import eu.timepit.refined.types.numeric.{PosInt, PosLong}
import io.circe.{Decoder, Encoder}
import io.circe.refined._
import scalaz.deriving

@deriving(Encoder, Decoder)
case class OrderedSushi(amount: PosInt, kind: String, price: PosLong, orderHandle: OrderHandle)

@deriving(Encoder, Decoder)
case class OrderHandle(id: UUID)
