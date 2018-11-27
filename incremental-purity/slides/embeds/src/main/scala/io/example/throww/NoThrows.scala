package io.example.throww
import java.util.UUID

import cats.implicits._
import cats.effect.IO

object NoThrows {

  def registerUser(user: User): Either[String, IO[String]] = {
    validate(user).map(_ => save(user).map(_.id))
  }

  private def validate(user: User): Either[String, Unit] =
    Either.cond(user.name.nonEmpty, (), "foo")

  private def save(user: User): IO[User] = {
    val logSaving = IO(println("saving user"))
    val getNewId  = IO(UUID.randomUUID().toString)

    logSaving *> getNewId.map(newId => user.copy(id = newId))
  }
}
