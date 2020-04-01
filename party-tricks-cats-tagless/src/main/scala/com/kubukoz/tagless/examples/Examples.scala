package com.kubukoz.tagless.examples

import cats.effect.IO
import cats.~>
import cats.tagless.implicits._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import cats.data.EitherT
import doobie.Transactor
import doobie.ConnectionIO
import cats.data.WriterT
import cats.data.Tuple2K
import cats.effect.Concurrent
import cats.effect.implicits._
import doobie.implicits._
import scala.concurrent.duration._
import java.time.Instant
import java.time.Duration
import cats.effect.Timer
import cats.effect.concurrent.Semaphore
import cats.tagless.diagnosis.Instrumentation
import natchez.Trace
import cats.tagless.diagnosis.Instrument
import cats.tagless.FunctorK
import cats.tagless.autoFunctorK
import cats.tagless.autoInstrument

object Examples {

  def withLogging(logger: Logger[IO], example: UserService[IO]): UserService[IO] = {

    val addLogging: IO ~> IO = λ[IO ~> IO] { action =>
      logger.info("Making call") *>
        action
          .attempt
          .flatTap(result => logger.info("Call resulted in " + result))
          .rethrow
    }

    example.mapK(addLogging)
  }

  def liftErrors(example: UserService[IO]): UserService[EitherT[IO, Throwable, *]] = {

    val catchToEither: IO ~> EitherT[IO, Throwable, *] =
      λ[IO ~> EitherT[IO, Throwable, *]](action => EitherT(action.attempt))

    example.mapK(catchToEither)
  }

  def transacted(
    example: UserService[ConnectionIO],
    transactor: Transactor[IO]
  ): UserService[IO] = {
    val transact: ConnectionIO ~> IO = transactor.trans

    example.mapK(transact)
  }

  def sendingMessages[Messages](
    example: UserService[WriterT[IO, Messages, *]]
  )(
    sender: Messages => IO[Unit]
  ): UserService[IO] = {
    val send: WriterT[IO, Messages, *] ~> IO =
      λ[WriterT[IO, Messages, *] ~> IO] {
        _.run.flatMap(_.leftTraverse(sender).map(_._2))
      }

    example.mapK(send)
  }

  def fallback(main: UserService[IO], backup: UserService[IO]): UserService[IO] =
    main.map2K(backup)(
      λ[Tuple2K[IO, IO, *] ~> IO](action => action.first orElse action.second)
    )

  def raceBoth(
    main: UserService[IO],
    backup: UserService[IO]
  )(
    implicit conc: Concurrent[IO]
  ): UserService[IO] =
    main.map2K(backup)(
      λ[Tuple2K[IO, IO, *] ~> IO](action => action.first.race(action.second).map(_.merge))
    )

  def mergeBoth(
    redis: UserService[IO],
    postgres: UserService[ConnectionIO],
    inMemory: UserService[cats.Id]
  )(
    transactor: Transactor[IO]
  ) = {
    val combined = UserService.product3K(redis, postgres, inMemory)

    // We can't (yet) combine the results as we don't have any information about the types :(
    combined.findAll match {
      case (redisCall, postgresCall, constantResult) =>
        redisCall |+|
          postgresCall.transact(transactor) |+|
          IO.pure(constantResult)
    }
  }

  val liftInMemoryInstance: UserService[IO] = {
    val instance: UserService[cats.Id] = UserService.constant
    val lift: cats.Id ~> IO = λ[cats.Id ~> IO](IO.pure(_))

    instance.mapK(lift)
  }

  type WithTime[A] = WriterT[IO, FiniteDuration, A]

  def timedCalls(instance: UserService[IO]): UserService[WithTime] = {

    val makeTimed: IO ~> WithTime = λ[IO ~> WithTime] { action =>
      val now = IO(Instant.now())
      WriterT {
        (now, action, now).mapN { (before, result, after) =>
          val totalTime = Duration.between(before, after).toMillis().millis

          (totalTime, result)
        }
      }
    }

    instance.mapK(makeTimed)
  }

  def repeatingExponentially(
    instance: UserService[IO]
  )(
    implicit timer: Timer[IO]
  ): UserService[fs2.Stream[IO, *]] = {

    val liftToStream: IO ~> fs2.Stream[IO, *] = λ[IO ~> fs2.Stream[IO, *]] { action =>
      fs2
        .Stream
        .repeatEval(action)
        .zipLeft(fs2.Stream.iterate(10.millis)(_ * 2).evalMap(IO.sleep))
    }

    instance.mapK(liftToStream)
  }

  def withSemaphore(instance: UserService[IO])(sem: Semaphore[IO]): UserService[IO] =
    instance.mapK(λ[IO ~> IO](sem.withPermit(_)))

  def retryOnce(instance: UserService[IO]): UserService[IO] =
    instance.mapK(λ[IO ~> IO](action => action.orElse(action)))

  def logMethodName(instance: UserService[IO], logger: Logger[IO]): UserService[IO] =
    instance.instrument.mapK {
      λ[Instrumentation[IO, *] ~> IO] { inst =>
        logger.info(show"Running ${inst.algebraName}.${inst.methodName}") *> inst.value
      }
    }

  def withDisabledMethods(
    instance: UserService[IO]
  )(
    methodNames: String*
  ): UserService[IO] = {
    def disabledMethod[A] =
      methodNames.toSet.compose[Instrumentation[IO, A]](_.methodName)

    instance.instrument.mapK {

      λ[Instrumentation[IO, *] ~> IO] {
        case inst if disabledMethod(inst) =>
          IO.raiseError(
            new Throwable(show"Disabled: ${inst.algebraName}.${inst.methodName}")
          )

        case inst => inst.value
      }
    }
  }

  def traced[F[_]: Trace](instance: UserService[F]): UserService[F] =
    instance.instrument.mapK {
      λ[Instrumentation[F, *] ~> F] { inst =>
        Trace[F].span(inst.algebraName + "." + inst.methodName)(inst.value)
      }
    }

  def tracedAbstract[Alg[_[_]]: Instrument: FunctorK, F[_]: Trace](
    instance: Alg[F]
  ): Alg[F] =
    instance.instrument.mapK {
      λ[Instrumentation[F, *] ~> F] { inst =>
        Trace[F].span(inst.algebraName + "." + inst.methodName)(inst.value)
      }
    }

  implicit class TracedAlgSyntax[Alg[_[_]], F[_]](private val instance: Alg[F])
    extends AnyVal {

    def spanByMethod(
      implicit F: Trace[F],
      instrument: Instrument[Alg],
      functorK: FunctorK[Alg]
    ): Alg[F] = tracedAbstract(instance)
  }
}
