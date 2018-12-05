package io.example.fake

import cats.{~>, Apply, FlatMap}

trait NonEmptyParallel[M[_], F[_]] extends Serializable {

  def apply: Apply[F]
  def flatMap: FlatMap[M]

  def sequential: F ~> M
  def parallel: M ~> F
}

trait Parallel[M[_], F[_]] extends cats.NonEmptyParallel[M, F] {

  def applicative: cats.Applicative[F]
  def monad: cats.Monad[M]

  override def apply: Apply[F]     = applicative
  override def flatMap: FlatMap[M] = monad
}
