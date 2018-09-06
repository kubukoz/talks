trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

//🙅‍♂🙄🤔️
implicit def eitherFunctor[E]
  : Functor[({ type L[A] = Either[E, A] })#L]

//😤👌💯
implicit def eitherFunctor[E]: Functor[Either[E, ?]]

//example from earlier
def g[F[_]](ints: F[Int]): F[Int] = ints

g[Either[String, ?]](Right(6))
