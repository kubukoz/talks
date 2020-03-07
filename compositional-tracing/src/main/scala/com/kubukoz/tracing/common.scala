package com.kubukoz.tracing

import cats.effect.ConcurrentEffect
import cats.effect.ContextShift
import cats.implicits._
import cats.effect.implicits._
import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.Async

object common {

  //Universally quantified (forall A) runWithContext. Scala 2 doesn't have polymorphic lambdas, so.
  trait UnsafeRunWithContext[Context] {
    def runWithContext[A](ctx: Context)(f: => A): A
  }

  def runFWithContext[F[_]: ConcurrentEffect: ContextShift, Context, A](
    unsafeRunWithContext: UnsafeRunWithContext[Context],
    getContext: F[Context]
  )(
    ctx: Context
  )(
    action: F[A]
  ): F[A] =
    getContext.flatMap { oldContext =>
      //this is basically forking the IO underneath (with the chosen context)
      Concurrent[F]
        .cancelable[A] { cb =>
          unsafeRunWithContext.runWithContext(ctx) {
            action.runCancelable(e => IO(cb(e))).unsafeRunSync()
          }
        }
        //after the forked IO is done, we need to run the rest of the program with the old context!
        .guarantee(
          ContextShift[F].shift *>
            Async[F].async[Unit] { cb =>
              unsafeRunWithContext.runWithContext(oldContext) {
                cb(Right(()))
              }
            }
        )
    }

}
