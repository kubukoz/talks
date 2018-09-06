val liftToIO: IO[Int] = IO.pure(42)

val random: IO[Int] = { IO(scala.util.Random.nextInt(100)) }

val incUntilGtRandom: IO[Int] = {
  for {
    a    <- mutable.incGet
    rand <- random
    result <- {
      if (a > rand) IO.pure(a)
      else incUntilGtRandom
    }
  } yield result
}
