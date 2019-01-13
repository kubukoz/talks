package com.app.orders
import io.circe.{Decoder, Encoder}
import scalaz.deriving

@deriving(Encoder, Decoder)
sealed trait OrderStatus extends Product with Serializable

object OrderStatus {
  case object InProgress                         extends OrderStatus
  case object Complete                           extends OrderStatus
  case class Canceled(reason: CancelationReason) extends OrderStatus

  @deriving(Encoder, Decoder)
  sealed trait CancelationReason extends Product with Serializable

  object CancelationReason {
    case object MissingStock extends CancelationReason
    case object Unknown      extends CancelationReason
  }
}
