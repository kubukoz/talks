import cats.~>

trait Parallel[M[_], F[_]] {
  def applicative: Applicative[F]
  def monad: Monad[M]

  def sequential: F ~> M
  def parallel:   M ~> F

  def parProduct[A, B](ma: M[A], mb: M[B]): M[(A, B)] = {
    (ma.parallel, mb.parallel).product.sequential
  }
}
