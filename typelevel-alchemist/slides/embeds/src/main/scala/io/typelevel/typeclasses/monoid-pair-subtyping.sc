final case class Pair[A <: Monoid[A], B <: Monoid[B]]
                     (first: A, second: B) extends Monoid[Pair[A, B]] {
  def empty: Pair[A, B] = ???

  def combine(x: Pair[A, B], y: Pair[A, B]): Pair[A, B] = ???
}
