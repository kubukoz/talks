package io.example.logging

import cats.FlatMap
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.tagless.finalAlg
import io.chrisdavenport.log4cats.Logger
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

class LoggedService[F[_]: Logger: UserRepository: FlatMap] {

  val findAllUsers: F[List[User]] = for {
    _     <- Logger[F].info("Looking for users")
    users <- UserRepository[F].findAll
    _     <- Logger[F].info(show"Found users: $users")
  } yield users
}

object LoggingExample extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    app[IO].as(ExitCode.Success)
  }

  def app[F[_]: Sync]: F[Unit] = {
    implicit val repo: UserRepository[F] = new UserRepository[F] {
      override val findAll: F[List[User]] = List(User("A"), User("B")).pure[F]
    }

    for {
      implicit0(logger: Logger[F]) <- Slf4jLogger.create[F]
      _                            <- new LoggedService[F].findAllUsers
    } yield ()
  }

  //polyfill for better support of better-monadic-for in IDEA
  object implicit0 {
    def unapply[A](a: A): Some[A] = Some(a)
  }
}

@finalAlg
trait UserRepository[F[_]] {
  def findAll: F[List[User]]
}
