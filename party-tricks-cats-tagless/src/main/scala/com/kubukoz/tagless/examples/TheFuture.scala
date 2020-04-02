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
import com.kubukoz.tagless.examples.TheFuture.Parameterized
import com.kubukoz.tagless.examples.TheFuture.WithParameters

object TheFuture {

  trait Parameter[Constraints[_]] {
    type Tpe
    def name: String
    def value: Tpe
    def constraints: Constraints[Tpe]
  }

  object Parameter {
    type Aux[Constraints[_], Tpe_] = Parameter[Constraints] { type Tpe = Tpe_ }

    def of[Tpe0, Constraints[_]](
      theName: String,
      theValue: Tpe0,
      theConstraints: Constraints[Tpe0]
    ): Parameter.Aux[Constraints, Tpe0] = new Parameter[Constraints] {
      type Tpe = Tpe0
      val name: String = theName
      val value: Tpe = theValue
      val constraints: Constraints[Tpe] = theConstraints
    }
  }

  final case class WithParameters[F[_], Constraints[_], A](
    value: F[A],
    parameters: List[List[Parameter[Constraints]]]
  )

  @typeclass
  trait Parameterized[Alg[_[_]]] {
    type AllParameters[Constraints[_]] <: HList

    def parameterized[F[_], Constraints[_]](
      alg: Alg[F]
    )(
      implicit constraints: LiftAll.Aux[
        Constraints,
        AllParameters[cats.Id],
        AllParameters[Constraints]
      ]
    ): Alg[WithParameters[F, Constraints, *]]
  }

  object Parameterized {

    type Aux[Alg[_[_]], AllParameters_[_[_]] <: HList] =
      Parameterized[Alg] {
        type AllParameters[F[_]] = AllParameters_[F]
      }
  }

  final case class ReturnType[F[_], Constraints[_], A](
    value: F[A],
    constraints: Constraints[A]
  )

  @typeclass
  trait ReturnTyped[Alg[_[_]]] {
    type AllReturnTypes[Constraints[_]] <: HList

    def returnTyped[F[_], Constraints[_]](
      alg: Alg[F]
    )(
      implicit constraints: LiftAll.Aux[
        Constraints,
        AllReturnTypes[cats.Id],
        AllReturnTypes[Constraints]
      ]
    ): Alg[ReturnType[F, Constraints, *]]
  }

  object ReturnTyped {

    type Aux[Alg[_[_]], AllReturnTypes_[_[_]] <: HList] =
      ReturnTyped[Alg] {
        type AllReturnTypes[F[_]] = AllReturnTypes_[F]
      }
  }

  @autoApplyK
  trait Users[F[_]] {
    def withParameters(name: String, age: Int): F[List[String]]
    def byId(id: String): F[Option[Int]]
  }

  object Users {

    import shapeless._

    type RTInstances[Constraints[_]] =
      Constraints[Option[Int]] :: Constraints[List[String]] :: HNil

    type PInstances[Constraints[_]] =
      Constraints[String] :: Constraints[Int] :: HNil

    implicit val returnTypedUsers: ReturnTyped.Aux[Users, RTInstances] =
      new ReturnTyped[Users] {

        type AllReturnTypes[Constraints[_]] =
          RTInstances[Constraints]

        def returnTyped[F[_], Constraints[_]](
          alg: Users[F]
        )(
          implicit constraints: LiftAll.Aux[
            Constraints,
            AllReturnTypes[cats.Id],
            AllReturnTypes[Constraints]
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

    implicit val parameterizedUsers: Parameterized.Aux[Users, PInstances] =
      new Parameterized[Users] {

        type AllParameters[Constraints[_]] = PInstances[Constraints]

        def parameterized[F[_], Constraints[_]](
          alg: Users[F]
        )(
          implicit constraints: LiftAll.Aux[
            Constraints,
            AllParameters[cats.Id],
            AllParameters[Constraints]
          ]
        ): Users[WithParameters[F, Constraints, *]] =
          new Users[WithParameters[F, Constraints, *]] {

            def withParameters(
              name: String,
              age: Int
            ): WithParameters[F, Constraints, List[String]] =
              WithParameters(
                alg.withParameters(name, age),
                List(
                  List(
                    Parameter.of(
                      "name",
                      name,
                      constraints.instances.select[Constraints[String]]
                    ),
                    Parameter
                      .of("age", age, constraints.instances.select[Constraints[Int]])
                  )
                )
              )

            def byId(id: String): WithParameters[F, Constraints, Option[Int]] =
              WithParameters(
                alg.byId(id),
                List(
                  List(
                    Parameter.of(
                      "id",
                      id,
                      constraints.instances.select[Constraints[String]]
                    )
                  )
                )
              )
          }
      }
  }
}

object TheFutureDemo extends IOApp {
  import cats.implicits._

  def run(args: List[String]): IO[ExitCode] = {

    import com.kubukoz.tagless.examples.TheFuture.ReturnTyped.ops._
    import com.kubukoz.tagless.examples.TheFuture.Parameterized.ops._

    import cats.~>
    import cats.tagless.implicits._

    import shapeless._

    def logParameters[
      F[_]: FlatMap: cats.effect.Console,
      Alg[_[_]]: FunctorK,
      ParamsWithInstances[_[_]] <: HList
    ](
      tag: String,
      alg: Alg[F]
    )(
      implicit returnTyped: Parameterized.Aux[Alg, ParamsWithInstances],
      liftParamsToShow: LiftAll.Aux[Show, ParamsWithInstances[cats.Id], ParamsWithInstances[
        Show
      ]]
    ): Alg[F] =
      alg
        .parameterized[Show]
        .mapK(
          λ[WithParameters[F, Show, *] ~> F] { rt =>
            val paramString = rt
              .parameters
              .map { paramList =>
                paramList
                  .map(param => param.name + " = " + param.constraints.show(param.value))
                  .mkString("(", ", ", ")")
              }
              .mkString

            cats.effect.Console[F].putStrLn(show"[$tag] Parameters were: $paramString") *>
              rt.value
          }
        )

    def logResult[
      F[_]: FlatMap: cats.effect.Console,
      Alg[_[_]]: FunctorK,
      ParamsWithInstances[_[_]] <: HList
    ](
      tag: String,
      alg: Alg[F]
    )(
      implicit returnTyped: ReturnTyped.Aux[Alg, ParamsWithInstances],
      liftParamsToShow: LiftAll.Aux[Show, ParamsWithInstances[cats.Id], ParamsWithInstances[
        Show
      ]]
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
      ParamsWithInstances[_[_]] <: HList
    ](
      alg1: Alg[F],
      alg2: Alg[F]
    )(
      implicit returnTyped: ReturnTyped.Aux[Alg, ParamsWithInstances],
      liftParamsToShow: LiftAll.Aux[Semigroup, ParamsWithInstances[cats.Id], ParamsWithInstances[
        Semigroup
      ]]
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

    logParameters("main", logResult("main", combineResults(loggedInstance, users2)))
      .withParameters("hello", 42)
      .as(ExitCode.Success)
  }
}
