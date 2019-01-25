package io.example.doobie

import cats.effect._
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

import scala.concurrent.ExecutionContext

//stubs
object IOImports {
  implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] =
    IO.timer(ExecutionContext.global)
}
import IOImports._

//actual code

object TransactorEx {

val transactor: Resource[IO, HikariTransactor[IO]] = for {
  connectEc  <- ExecutionContexts.fixedThreadPool[IO](size = 10)
  transactEc <- ExecutionContexts.cachedThreadPool[IO]

  xa <- HikariTransactor.newHikariTransactor[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost/postgres",
    "postgres",
    "postgres",
    connectEc,
    transactEc)
} yield xa
}
