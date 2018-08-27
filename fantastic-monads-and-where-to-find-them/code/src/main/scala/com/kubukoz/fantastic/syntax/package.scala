package com.kubukoz.fantastic

import cats.{Id, ~>}
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Future
import scala.language.higherKinds

package object syntax {
  type ToFuture[F[_]] = F ~> Future

  implicit class ToFutureSyntax[F[_], T](val value: F[T]) extends AnyVal {
    def toFuture(implicit ev: ToFuture[F]): Future[T] = ev(value)
  }

  implicit def taskToFuture(implicit sch: Scheduler): ToFuture[Task] = λ[Task ~> Future](_.runAsync)
  implicit val idToFuture: ToFuture[Id] = λ[Id ~> Future](Future.successful(_))
}
