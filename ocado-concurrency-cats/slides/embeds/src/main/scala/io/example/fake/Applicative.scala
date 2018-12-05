package io.example.fake

trait Applicative[F[_]] extends cats.Functor[F]{

  //wrap a pure value in F[_]
  def pure[A](a: A): F[A]

  //run two independent F[_] values together,
  //combine them with `f`
  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
}
