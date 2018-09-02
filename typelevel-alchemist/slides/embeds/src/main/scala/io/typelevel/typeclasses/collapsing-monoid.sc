trait Monoid[T] {
  def empty: T
  def combine(x: T, y: T): T
}

object Monoid {
  def instance[T](emptyT: T, combineT: (T, T) => T): Monoid[T] =
  /* exercise for the reader*/ ???
}

def collapseList[T](list: List[T])(implicit M: Monoid[T]): T =
  list.foldRight(M.empty)(M.combine)
