//also called ~> or NaturalTransformation
trait FunctionK[F[_], G[_]] {

  /**
    * Applies this functor transformation from `F` to `G`
    */
  def apply[A](fa: F[A]): G[A]
}
