package io.example.dbhttp

import cats.effect._
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.syntax._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._

object Main extends IOApp {

  val helloWorldService = HttpRoutes
    .of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
    }

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(helloWorldService.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
