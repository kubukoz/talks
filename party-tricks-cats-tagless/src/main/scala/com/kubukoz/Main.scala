package com.kubukoz

import cats.implicits._
import cats.tagless.implicits._
import cats.tagless.finalAlg
import cats.tagless.autoInstrument
import cats.tagless.diagnosis.Instrumentation
import cats.tagless.diagnosis.Instrument
import cats.tagless.autoFunctorK
import cats.tagless.FunctorK
import cats.~>
import cats.FlatMap
import cats.effect.IO
import cats.effect.Console
import cats.tagless.autoProductNK
import cats.effect.IOApp
import cats.effect.ExitCode
import tagless.examples._
import natchez.log.Log
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import natchez.Span
import cats.data.Kleisli

@finalAlg
@autoInstrument
@autoProductNK
@autoFunctorK
trait Foo[F[_]] {
  def bar(a: Int): F[Int]
}

//has to be there
object Foo {}

object Main extends IOApp {

  import Console.io._

  import fs2.Stream

  val fooIO: Foo[IO] = new Foo[IO] {
    def bar(a: Int): IO[Int] = putStrLn(show"bar($a)").as(42)
  }

  def logged[Alg[_[_]]: Instrument: FunctorK, F[_]: Console: FlatMap](
    alg: Alg[F]
  ): Alg[F] =
    Instrument[Alg]
      .instrument(alg)
      .mapK(
        λ[Instrumentation[F, *] ~> F](i =>
          Console[F].putStrLn(show"${i.algebraName}.${i.methodName}") *> i.value
        )
      )

  import scala.concurrent.duration._

  val streaming: IO ~> Stream[IO, *] =
    λ[IO ~> Stream[IO, *]](
      Stream.repeatEval(_) zipLeft Stream.iterate(100.millis)(_ * 2).evalMap(IO.sleep)
    )

  import Examples.TracedAlgSyntax

  def run(args: List[String]): IO[ExitCode] = {
    implicit val logger = Slf4jLogger.getLogger[IO]
    val ep = Log.entryPoint[IO]("demo")

    type Traced[A] = Kleisli[IO, Span[IO], A]
    val liftTraced: IO ~> Traced = Kleisli.liftK

    implicit val tracedLogger = logger.mapK(liftTraced)

    val users =
      UserService
        .constant
        .mapK(λ[cats.Id ~> IO](IO.pure(_)) andThen liftTraced)
        .spanByMethod

    val mail =
      MailService.logging[Traced].spanByMethod

    val main =
      MainService.instance(users, mail).spanByMethod

    ep.root("example").use(main.sendMessageToEach("hello").run)
  }.as(ExitCode.Success)

}
