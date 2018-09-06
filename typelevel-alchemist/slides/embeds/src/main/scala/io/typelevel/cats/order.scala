//Comparison = GreaterThan | EqualTo | LessThan

trait Order[A] extends Eq[A] {
  //syntax like a <= b
  def comparison(x: A, y: A): Comparison
}
