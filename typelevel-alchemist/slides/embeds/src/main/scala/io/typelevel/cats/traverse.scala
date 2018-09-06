trait Traverse[F[_]] extends Functor[F] {

  /**
    * Thread all the G effects through the F structure
    * to invert the structure from F[G[A]] to G[F[A]].
    */
  def sequence[G[_]: Applicative, A](fga: F[G[A]]): G[F[A]]

  /**
    * Given a function which returns a G effect,
    * thread this effect through the running of this function
    * on all the values in F, returning an F[B] in a G context.
    */
  def traverse[G[_]: Applicative, A, B](fa: F[A])(
    f: A => G[B]): G[F[B]] = fa.map(f).sequence
}
