trait Semigroup[A] {

  /**
    * Associative operation which combines two values.
    */
  def combine(x: A, y: A): A
}

trait Monoid[A] extends Semigroup[A] {

  /**
    * Return the identity element for this monoid.
    */
  def empty: A
}
