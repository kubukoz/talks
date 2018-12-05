package io.example.fake.realer

trait FlatMap[F[_]] extends cats.Apply[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    flatMap(fa)(a => map(fb)((a, _)))
}

trait Monad[F[_]] extends FlatMap[F] with cats.Applicative[F]
