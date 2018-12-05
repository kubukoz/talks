package io.example.samples

import cats.Applicative

object MapApply {

  implicit def mapApplicative[K]: Applicative[Map[K, ?]] = new Applicative[Map[K, ?]] {

    def pure[V](v: V): Map[K, V] = ??? //oops

    override def ap[A, B](ff: Map[K, A => B])(fa: Map[K, A]): Map[K, B] = ??? //can be implemented
  }
}
