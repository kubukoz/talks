package io.example.fake.realer

//the real(er) Slim Shady
trait Apply[F[_]] extends cats.Functor[F] {
  def ap[A, B](fa: F[A => B])(fb: F[A]): F[B]

  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    ap(map(fa)(a => f(a, _: B)))(fb)
}

trait Applicative[F[_]] extends Apply[F] {
  def pure[A](a: A): F[A]
}
