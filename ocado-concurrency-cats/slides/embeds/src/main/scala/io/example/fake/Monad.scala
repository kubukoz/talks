package io.example.fake

trait Monad[F[_]] extends cats.Applicative[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}
