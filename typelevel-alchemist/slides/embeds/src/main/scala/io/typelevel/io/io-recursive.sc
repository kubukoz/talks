import cats.effect.IO

//stack safe, will only blow up the heap because of the trailing `map`
val program: IO[Int] =
  putStrLn("foo").flatMap(_ => program).map(_ + 1)
