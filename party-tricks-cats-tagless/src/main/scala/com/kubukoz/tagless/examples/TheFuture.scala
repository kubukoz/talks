package com.kubukoz.tagless.examples

import simulacrum.typeclass
import shapeless.HList
import shapeless.ops.hlist.LiftAll
import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import com.kubukoz.tagless.examples.TheFuture.Users
import cats.Show
import shapeless.HNil
import com.kubukoz.tagless.examples.TheFuture.ReturnTyped
import cats.FlatMap
import cats.tagless.FunctorK
import com.kubukoz.tagless.examples.TheFuture.ReturnType
import cats.Apply
import cats.tagless.ApplyK
import cats.tagless.autoApplyK
import cats.data.Tuple2K
import cats.kernel.Semigroup

object TheFuture {

  trait Parameter[Constraints[_]] {
    type Tpe
    def name: String
    def value: Tpe
    def constraints: Constraints[Tpe]
  }

  trait WithParameters[F[_], Constraints[_], A] {
    def parameters: List[List[Parameter[Constraints]]]
    def value: F[A]
  }

  @typeclass
  trait Parameterized[Alg[_[_]]] {

    def parameterized[F[_], Constraints[_]](
      alg: Alg[F]
    ): Alg[WithParameters[F, Constraints, *]]
  }

  final case class ReturnType[F[_], Constraints[_], A](
    value: F[A],
    constraints: Constraints[A]
  )

  @typeclass
  trait ReturnTyped[Alg[_[_]]] {
    type AllReturnTypes <: HList
    type AllReturnTypesWithInstances[Constraints[_]] <: HList

    def returnTyped[F[_], Constraints[_]](
      alg: Alg[F]
    )(
      implicit constraints: LiftAll.Aux[
        Constraints,
        AllReturnTypes,
        AllReturnTypesWithInstances[Constraints]
      ]
    ): Alg[ReturnType[F, Constraints, *]]
  }

  object ReturnTyped {

    type Aux[Alg[_[_]], AllReturnTypes_ <: HList, AllReturnTypesWithInstances_[_[_]] <: HList] =
      ReturnTyped[Alg] {
        type AllReturnTypes = AllReturnTypes_
        type AllReturnTypesWithInstances[F[_]] = AllReturnTypesWithInstances_[F]
      }
  }

  @autoApplyK
  trait Users[F[_]] {
    def withParameters(name: String, age: Int): F[List[String]]
    def byId(id: String): F[Option[Int]]
  }

  object Users {

    import shapeless._

    type RTs = Option[Int] :: List[String] :: HNil

    type RTInstances[Constraints[_]] =
      Constraints[Option[Int]] :: Constraints[List[String]] :: HNil

    implicit val returnTypedUsers: ReturnTyped.Aux[Users, RTs, RTInstances] =
      new ReturnTyped[Users] {

        type AllReturnTypes = RTs

        type AllReturnTypesWithInstances[Constraints[_]] =
          RTInstances[Constraints]

        def returnTyped[F[_], Constraints[_]](
          alg: Users[F]
        )(
          implicit constraints: LiftAll.Aux[
            Constraints,
            AllReturnTypes,
            AllReturnTypesWithInstances[Constraints]
          ]
        ): Users[ReturnType[F, Constraints, *]] =
          new Users[ReturnType[F, Constraints, *]] {

            def withParameters(
              name: String,
              age: Int
            ): ReturnType[F, Constraints, List[String]] =
              ReturnType(
                alg.withParameters(name, age),
                constraints.instances.select[Constraints[List[String]]]
              )

            def byId(id: String): ReturnType[F, Constraints, Option[Int]] =
              ReturnType(
                alg.byId(id),
                constraints.instances.select[Constraints[Option[Int]]]
              )
          }
      }
  }
}

object TheFutureDemo extends IOApp {
  import cats.implicits._

  def run(args: List[String]): IO[ExitCode] = {

    import com.kubukoz.tagless.examples.TheFuture.ReturnTyped.ops._

    import cats.~>
    import cats.tagless.implicits._

    import shapeless._

    def logResult[
      F[_]: FlatMap: cats.effect.Console,
      Alg[_[_]]: FunctorK,
      Params <: HList,
      ParamsWithInstances[_[_]] <: HList
    ](
      tag: String,
      alg: Alg[F]
    )(
      implicit returnTyped: ReturnTyped.Aux[Alg, Params, ParamsWithInstances],
      liftParamsToShow: LiftAll.Aux[Show, Params, ParamsWithInstances[Show]]
    ): Alg[F] =
      alg
        .returnTyped[Show]
        .mapK(
          λ[ReturnType[F, Show, *] ~> F] { rt =>
            rt.value.flatTap { result =>
              implicit val instance = rt.constraints

              cats.effect.Console[F].putStrLn(show"[$tag] Result was: $result")
            }
          }
        )

    def combineResults[
      F[_]: Apply,
      Alg[_[_]]: ApplyK,
      Params <: HList,
      ParamsWithInstances[_[_]] <: HList
    ](
      alg1: Alg[F],
      alg2: Alg[F]
    )(
      implicit returnTyped: ReturnTyped.Aux[Alg, Params, ParamsWithInstances],
      liftParamsToShow: LiftAll.Aux[Semigroup, Params, ParamsWithInstances[Semigroup]]
    ): Alg[F] =
      alg1
        .returnTyped[Semigroup]
        .productK(alg2.returnTyped[Semigroup])
        .mapK(
          λ[Tuple2K[ReturnType[F, Semigroup, *], ReturnType[F, Semigroup, *], *] ~> F] {
            rt =>
              implicit val theMonoid = rt.first.constraints

              (rt.first.value, rt.second.value).mapN(_ |+| _)
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

      def byId(id: String): IO[Option[Int]] = IO.pure(None)
    }

    val loggedInstance = logResult("users", users)

    logResult("main", combineResults(loggedInstance, users2))
      .withParameters("hello", 42)
      .as(ExitCode.Success)
  }
}
