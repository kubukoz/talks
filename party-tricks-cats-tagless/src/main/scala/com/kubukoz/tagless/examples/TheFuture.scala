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
import cats.Apply
import cats.tagless.ApplyK
import cats.tagless.autoApplyK
import cats.data.Tuple2K
import cats.kernel.Semigroup
import com.kubukoz.tagless.examples.TheFuture.Parameterized
import com.kubukoz.tagless.examples.TheFuture.WithParameters
import cats.data.NonEmptyList
import cats.Monad
import cats.tagless.autoFunctorK

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

  //todo needs rename
  final case class WithParameters[Constraints[_], A](
    parameters: List[List[Parameter[Constraints]]],
    implicits: Option[NonEmptyList[Parameter[Constraints]]]
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
    ): Alg[WithParameters[Constraints, *]]
  }

  object Parameterized {

    type Aux[Alg[_[_]], AllParameters_[_[_]] <: HList] =
      Parameterized[Alg] {
        type AllParameters[Constraints[_]] = AllParameters_[Constraints]
      }
  }

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
    ): Alg[Constraints]
  }

  object ReturnTyped {

    type Aux[Alg[_[_]], AllReturnTypes_[_[_]] <: HList] =
      ReturnTyped[Alg] {
        type AllReturnTypes[F[_]] = AllReturnTypes_[F]
      }
  }

  trait TC[A]

  object TC {
    implicit def showTC[A]: Show[TC[A]] = _ => "TC[from the Show instance]"
    implicit def forAny[A]: TC[A] = new TC[A] {}
  }

  @autoApplyK
  trait Users[F[_]] {
    def withParameters(name: String, age: Int): F[List[String]]
    def byId(id: String): F[Option[Int]]
    def withImplicit(s: String)(implicit n: TC[String]): F[String]
  }

  object Users {

    import shapeless._

    type RTInstances[Constraints[_]] =
      Constraints[Option[Int]] :: Constraints[List[String]] :: Constraints[String] :: HNil

    type PInstances[Constraints[_]] =
      Constraints[String] :: Constraints[Int] :: Constraints[TC[String]] :: HNil

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
        ): Users[Constraints] =
          new Users[Constraints] {

            def withParameters(name: String, age: Int): Constraints[List[String]] =
              constraints.instances.select[Constraints[List[String]]]

            def byId(id: String): Constraints[Option[Int]] =
              constraints.instances.select[Constraints[Option[Int]]]

            def withImplicit(s: String)(implicit n: TC[String]): Constraints[String] =
              constraints.instances.select[Constraints[String]]
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
        ): Users[WithParameters[Constraints, *]] =
          new Users[WithParameters[Constraints, *]] {

            def withParameters(
              name: String,
              age: Int
            ): WithParameters[Constraints, List[String]] =
              WithParameters(
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
                ),
                None
              )

            def byId(id: String): WithParameters[Constraints, Option[Int]] =
              WithParameters(
                List(
                  List(
                    Parameter.of(
                      "id",
                      id,
                      constraints.instances.select[Constraints[String]]
                    )
                  )
                ),
                None
              )

            def withImplicit(
              s: String
            )(
              implicit n: TC[String]
            ): WithParameters[Constraints, String] = WithParameters(
              List(
                List(
                  Parameter.of("s", s, constraints.instances.select[Constraints[String]])
                )
              ),
              Some(
                NonEmptyList.one(
                  Parameter
                    .of("n", n, constraints.instances.select[Constraints[TC[String]]])
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
      Alg[_[_]]: ApplyK,
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
      alg.map2K(alg.parameterized[Show])(
        λ[Tuple2K[F, WithParameters[Show, *], *] ~> F] {
          case Tuple2K(action, params) =>
            val paramString = params
              .parameters
              .map {
                _.map(param => param.name + " = " + param.constraints.show(param.value))
                  .mkString("(", ", ", ")")
              }
              .mkString

            val implicitParamString = params.implicits.foldMap {
              _.map(param => param.name + " = " + param.constraints.show(param.value))
                .mkString_("(implicit ", ", ", ")")
            }
            val fullParamString = paramString ++ implicitParamString

            cats
              .effect
              .Console[F]
              .putStrLn(show"[$tag] Parameters were: $fullParamString") *> action
        }
      )

    def logResult[
      F[_]: FlatMap: cats.effect.Console,
      Alg[_[_]]: ApplyK,
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
      alg.map2K(alg.returnTyped[Show])(
        λ[Tuple2K[F, Show, *] ~> F] {
          case Tuple2K(value, rt) =>
            value.flatTap { result =>
              implicit val instance = rt

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
        .productK(alg2)
        .map2K(alg1.returnTyped[Semigroup])(
          λ[Tuple2K[Tuple2K[F, F, *], Semigroup, *] ~> F] {
            case Tuple2K(Tuple2K(left, right), rt) =>
              implicit val theSemigroup = rt

              (left, right).mapN(_ |+| _)
          }
        )

    import cats.effect.Console.implicits._

    val users: Users[IO] = new Users[IO] {
      def withParameters(name: String, age: Int): IO[List[String]] =
        IO.pure(List("foo", "bar"))

      def byId(id: String): IO[Option[Int]] = IO.pure(Some(42))

      def withImplicit(s: String)(implicit n: TheFuture.TC[String]): IO[String] =
        IO.pure("foo")
    }

    val users2: Users[IO] = new Users[IO] {
      def withParameters(name: String, age: Int): IO[List[String]] =
        IO.pure(List("baz", "qux"))

      def byId(id: String): IO[Option[Int]] = IO.pure(None)

      def withImplicit(s: String)(implicit n: TheFuture.TC[String]): IO[String] =
        IO.pure("boo")
    }

    val loggedInstance = logResult("users", users)

    val theInstance: Users[IO] =
      logParameters("main", logResult("main", combineResults(loggedInstance, users2)))

    theInstance.withImplicit("aaa").as(ExitCode.Success)
  }
}
