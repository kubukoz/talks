package com.app.orders

import io.circe.{Decoder, Encoder}
import scalaz.deriving

@deriving(Encoder, Decoder)
case class OrderedSushi(amount: Int, kind: String, price: Long)
