def sumInts(list: List[Int]): Int =
  list.foldRight(0)(_ + _)

def concatStrings(list: List[String]): String =
  list.foldRight("")(_ ++ _)

def unionSets[A](list: List[Set[A]]): Set[A] =
  list.foldRight(Set.empty[A])(_ union _)
