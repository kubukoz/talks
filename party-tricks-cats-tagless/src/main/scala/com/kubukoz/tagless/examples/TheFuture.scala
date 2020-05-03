package com.kubukoz.tagless.examples

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import com.kubukoz.tagless.examples.TheFuture.Users
import cats.Show
import cats.FlatMap
import cats.Apply
import cats.tagless.ApplyK
import cats.tagless.autoApplyK
import cats.data.Tuple2K
import cats.kernel.Semigroup
import cats.implicits._
import cats.tagless.Derive
import cats.tagless.diagnosis.Instrument
import cats.tagless.diagnosis.Instrumentation

object TheFuture {

  @autoApplyK
  trait Users[F[_]] {
    def withParameters(name: String, age: Int): F[List[String]]
    def byId(id: String): F[Option[Int]]
  }

  object Users {
    implicit val withShow = Derive.instrumentWith[Users, Show]
    implicit val withSemigroup = Derive.instrumentWith[Users, Semigroup]
  }
}

object TheFutureDemo extends IOApp {
  import cats.implicits._

  def run(args: List[String]): IO[ExitCode] = {
    import cats.~>
    import cats.tagless.implicits._

    import shapeless._

    def logParameters[
      F[_]: FlatMap: cats.effect.Console,
      Alg[_[_]]: ApplyK: Instrument.With[*[*[_]], Show]
    ](
      tag: String,
      alg: Alg[F]
    ): Alg[F] =
      alg
        .instrumentWith[Show]
        .mapK(
          λ[Instrumentation.With[F, Show, *] ~> F] { method =>
            val paramString = method
              .arguments
              .map {
                _.map(param => param.name + " = " + param.instance.show(param.value))
                  .mkString("(", ", ", ")")
              }
              .mkString

            cats
              .effect
              .Console[F]
              .putStrLn(
                show"[$tag] ${method.instrumentation.algebraName}.${method.instrumentation.methodName}$paramString"
              ) *> method.instrumentation.value
          }
        )

    def logResult[
      F[_]: FlatMap: cats.effect.Console,
      Alg[_[_]]: ApplyK: Instrument.With[*[*[_]], Show]
    ](
      tag: String,
      alg: Alg[F]
    ): Alg[F] =
      alg
        .instrumentWith[Show]
        .mapK(
          λ[Instrumentation.With[F, Show, *] ~> F] {
            case method =>
              method.instrumentation.value.flatTap { result =>
                implicit val instance = method.instance

                cats.effect.Console[F].putStrLn(show"[$tag] Result was: $result")
              }
          }
        )

    def combineResults[
      F[_]: Apply,
      Alg[_[_]]: ApplyK: Instrument.With[*[*[_]], Semigroup],
      ParamsWithInstances[_[_]] <: HList
    ](
      alg1: Alg[F],
      alg2: Alg[F]
    ): Alg[F] =
      alg1
        .instrumentWith[Semigroup]
        .map2K(alg2.instrumentWith[Semigroup])(
          λ[
            Tuple2K[
              Instrumentation.With[F, Semigroup, *],
              Instrumentation.With[F, Semigroup, *],
              *
            ] ~> F
          ] {
            case Tuple2K(left, right) =>
              implicit val theSemigroup = left.instance

              (left.instrumentation.value, right.instrumentation.value).mapN(_ |+| _)
          }
        )

    import cats.effect.Console.implicits._

    val users: Users[IO] = new Users[IO] {
      def withParameters(name: String, age: Int): IO[List[String]] =
        IO.pure(List("foo", "bar"))

      def byId(id: String): IO[Option[Int]] = IO.pure(Some(42))
    }

    val users2: Users[IO] = new Users[IO] {
      def withParameters(name: String, age: Int): IO[List[String]] =
        IO.pure(List("baz", "qux"))

      def byId(id: String): IO[Option[Int]] = IO.pure(Some(10))
    }

    val theInstance: Users[IO] =
      logParameters(
        "main",
        logResult(
          "main",
          combineResults(
            logResult("users", users),
            users2
          )
        )
      )

    theInstance.byId("aaa").as(ExitCode.Success)
  }
}
