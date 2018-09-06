trait Eq[A] {

  /**
    * Returns `true` if `x` and `y` are equivalent,
    * `false` otherwise.
    */
  def eqv(x: A, y: A): Boolean
}
