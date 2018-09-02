implicit val intMonoid: Monoid[Int] = Monoid.instance(0, _ + _)

implicit def setMonoid[T]: Monoid[Set[T]] =
  Monoid.instance(Set.empty, _ ++ _)

//Set("a", "b", "d")
collapseList(List(Set("a", "b"), Set("d")))

//6
collapseList(List(1, 2, 3))

//compilation failed: could not find
// implicit value for parameter M: cats.Monoid[Char]
collapseList(List('a', 'b', 'c'))

