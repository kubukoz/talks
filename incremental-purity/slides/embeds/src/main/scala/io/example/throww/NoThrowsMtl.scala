package io.example.throww

import java.util.UUID

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import cats.mtl.FunctorRaise

object NoThrowsMtl {
  type ThrowString[F[_]] = FunctorRaise[F, String]

  def registerUser[F[_]: Sync: ThrowString](user: User): F[String] = {
    validate[F](user) *>
      save[F](user).map(_.id)
  }

  private def validate[F[_]: Applicative: ThrowString](user: User): F[Unit] =
    if (user.name.nonEmpty) Applicative[F].unit
    else implicitly[ThrowString[F]].raise("foo")

  private def save[F[_]: Sync](user: User): F[User] = {
    val logSaving = Sync[F].delay(println("saving user"))
    val getNewId  = Sync[F].delay(UUID.randomUUID().toString)

    logSaving *> getNewId.map(newId => user.copy(id = newId))
  }
}
