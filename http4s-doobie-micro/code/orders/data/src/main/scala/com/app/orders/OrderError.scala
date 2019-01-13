package com.app.orders

import cats.{ErrorControl, MonadError}
import cats.data.NonEmptyList
import cats.implicits._
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import io.circe.Encoder

sealed trait OrderError extends Product with Serializable

object OrderError {
  type NelMonad[F[_]] = MonadError[F, NonEmptyList[OrderError]]
  def NelMonad[F[_]](implicit F: NelMonad[F]): NelMonad[F] = F

  type NelErrorHandler[F[_], G[_]] = ErrorControl[F, G, NonEmptyList[OrderError]]

  case class SushiKindNotFound(name: String)                                       extends OrderError
  case class SetNotDivisible(requested: PosInt, setSize: PosInt, lower: NonNegInt) extends OrderError

  val message: OrderError => String = {
    case SushiKindNotFound(name) => show"Couldn't find sushi kind: $name"
    case SetNotDivisible(requested, setSize, lower) =>
      val instead =
        if (lower.value > 0) show"${lower.value} or ${lower.value + setSize.value}"
        else show"${lower.value + setSize.value}"
      show"The requested sushi kind is sold in sets of size ${setSize.value} (requested ${requested.value}, so maybe try ordering $instead instead?)"
  }

  implicit val encoder: Encoder[OrderError] = Encoder[String].contramap(message)
}
