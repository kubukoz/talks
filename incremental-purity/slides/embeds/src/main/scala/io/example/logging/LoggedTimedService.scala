package io.example.logging

import java.util.concurrent.TimeUnit

import cats.Show
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import scalaz.deriving

import scala.concurrent.duration._

trait LoggedTimedService[F[_]] {
  def findUsers: F[List[User]]
}

object LoggedTimedService {

  def create[F[_]: Sync](implicit timer: Timer[F]): F[LoggedTimedService[F]] =
    Slf4jLogger.create[F].map { implicit L: Logger[F] =>
      new LoggedTimedService[F] {
        override val findUsers: F[List[User]] = {
          val action = timer.sleep(1.second).as(List(User("foo"), User("bar")))

          LoggingUtils.logLength(action)
        }
      }
    }
}

object LoggingUtils {

  def logLength[F[_], T, E](ft: F[T])(implicit
                                      logger: Logger[F],
                                      B: Bracket[F, E],
                                      clock: Clock[F]): F[T] = {
    val now = clock.realTime(TimeUnit.MILLISECONDS)

    now.flatMap { before =>
      val after = now.map(_ - before)

      def logTime(exitCase: ExitCase[E]): F[Unit] =
        after.flatMap(length => logger.info(s"Took $length milliseconds, exit case was $exitCase"))

      ft.guaranteeCase(logTime)
    }
  }
}

@deriving(Show)
case class User(name: String)

object LoggedServiceApp extends IOApp {
  import cats.effect.Console.io.putStrLn

  override def run(args: List[String]): IO[ExitCode] =
    LoggedTimedService
      .create[IO]
      .flatMap(_.findUsers)
      .flatMap {
        _.traverse(putStrLn(_))
      }
      .as(ExitCode.Success)
}
