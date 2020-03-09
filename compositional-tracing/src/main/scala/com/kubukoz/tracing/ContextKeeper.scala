package com.kubukoz.tracing

import cats.effect.ContextShift
import cats.implicits._
import cats.effect.implicits._
import cats.effect.Async
import cats.~>
import kamon.context.{Context => KamonContext}
import cats.effect.Sync
import kamon.Kamon
import cats.effect.Resource

trait ContextKeeper[F[_], Context] {
  def withContext: Context => F ~> F
  def keepContextAround: F ~> F
}

object ContextKeeper {

  //Universally quantified (forall A) runWithContext. Scala 2 doesn't have polymorphic lambdas, so.
  trait UnsafeRunWithContext[Context] {
    def runWithContext[A](ctx: Context)(f: => A): A
  }

  def instance[F[_]: Async: ContextShift, Context](
    unsafeRunWithContext: UnsafeRunWithContext[Context],
    getContext: F[Context]
  ): ContextKeeper[F, Context] = new ContextKeeper[F, Context] {

    private def shiftTo(ctx: Context): F[Unit] =
      ContextShift[F].shift *> Async[F].async[Unit] { cb =>
        unsafeRunWithContext.runWithContext(ctx)(cb(Right(())))
      }

    //Shift to the new context before the action, switch back after the action
    val withContext: Context => F ~> F = { ctx =>
      λ[F ~> F] { fa =>
        getContext.flatMap { oldContext =>
          Resource.make(shiftTo(ctx))(_ => shiftTo(oldContext)).use(_ => fa)
        }
      }
    }

    //After action, shift to the context we had before it
    val keepContextAround: F ~> F =
      λ[F ~> F](fa => getContext.map(shiftTo).flatMap(fa.guarantee))
  }

  implicit def kamonContextKeeper[
    F[_]: Async: ContextShift
  ]: ContextKeeper[F, KamonContext] = {
    val alternative = new ContextKeeper[F, KamonContext] {
      val withContext: KamonContext => F ~> F =
        ctx =>
          new (F ~> F) {

            def apply[A](fa: F[A]): F[A] =
              Sync[F].delay(Kamon.currentContext()).flatMap { oldContext =>
                Sync[F].delay(Kamon.storeContext(ctx)).bracket(_ => fa) { scope =>
                  Sync[F].delay(scope.close()) *> Sync[F]
                    .delay(Kamon.storeContext(oldContext))
                    .void
                }
              }
          }

      val keepContextAround: F ~> F =
        new (F ~> F) {

          def apply[A](fa: F[A]): F[A] = Sync[F].delay(Kamon.currentContext()).flatMap {
            oldContext => fa.guarantee(Sync[F].delay(Kamon.storeContext(oldContext)).void)
          }
        }

    }

    //todo: figure out if it's viable
    val _ = alternative

    val runWith = new UnsafeRunWithContext[KamonContext] {
      def runWithContext[X](context: KamonContext)(f: => X): X =
        Kamon.runWithContext(context)(f)
    }

    ContextKeeper.instance(runWith, Sync[F].delay(Kamon.currentContext()))
  }

}
