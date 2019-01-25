package io.example.doobie

import cats.Show
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.Encoder
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.blaze.BlazeServerBuilder
import scalaz.deriving
import io.circe.syntax._
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import TransactorEx._

@deriving(Show, Encoder)
case class Country(name: String, capital: String)

object DoobieMain extends IOApp with Http4sDsl[IO] {
  val streamCountries: Stream[ConnectionIO, Country] =
    sql"select c.name, c.capital from countries".query[Country].stream

  def countriesService(xa: Transactor[IO]): HttpApp[IO] =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "countries" =>
          Ok(streamCountries.transact(xa).map(_.asJson))
      }
      .orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    val server = for {
      xa <- transactor
      _  <- BlazeServerBuilder[IO].withHttpApp(countriesService(xa)).resource
    } yield ()

    //never completes normally (so the server won't shut down)
    server.use(_ => IO.never).as(ExitCode.Success)
  }
}
