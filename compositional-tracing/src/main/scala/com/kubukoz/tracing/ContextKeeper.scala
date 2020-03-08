package com.kubukoz.tracing

import cats.effect.ConcurrentEffect
import cats.effect.ContextShift
import cats.implicits._
import cats.effect.implicits._
import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.Async
import cats.~>
import kamon.context.{Context => KamonContext}
import cats.effect.Sync
import kamon.Kamon

trait ContextKeeper[F[_], Context] {
  def withContext: Context => F ~> F
  def keepContextAround: F ~> F
}

object ContextKeeper {

  //Universally quantified (forall A) runWithContext. Scala 2 doesn't have polymorphic lambdas, so.
  trait UnsafeRunWithContext[Context] {
    def runWithContext[A](ctx: Context)(f: => A): A
  }

  def instance[F[_]: ConcurrentEffect: ContextShift, Context](
    unsafeRunWithContext: UnsafeRunWithContext[Context],
    getContext: F[Context]
  ): ContextKeeper[F, Context] = new ContextKeeper[F, Context] {

    def withContext: Context => F ~> F =
      ctx =>
        new (F ~> F) {

          def apply[A](fa: F[A]): F[A] =
            getContext.flatMap { oldContext =>
              //this is basically forking the IO underneath (with the chosen context)
              Concurrent[F]
                .cancelable[A] { cb =>
                  unsafeRunWithContext.runWithContext(ctx) {
                    fa.runCancelable(e => IO(cb(e))).unsafeRunSync()
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

    def keepContextAround: F ~> F =
      λ[F ~> F](fa => getContext.flatMap(withContext(_)(fa)))
  }

  implicit def kamonContextKeeper[
    F[_]: ConcurrentEffect: ContextShift
  ]: ContextKeeper[F, KamonContext] = {
    val runWith = new UnsafeRunWithContext[KamonContext] {
      def runWithContext[X](context: KamonContext)(f: => X): X =
        Kamon.runWithContext(context)(f)
    }

    ContextKeeper.instance(runWith, Sync[F].delay(Kamon.currentContext()))
  }

}
