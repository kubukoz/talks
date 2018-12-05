package io.example.fake

import scala.concurrent.ExecutionContext

trait ContextShift[F[_]] {
  def shift: F[Unit]

  def evalOn[A](ec: ExecutionContext)(fa: F[A]): F[A]
}
