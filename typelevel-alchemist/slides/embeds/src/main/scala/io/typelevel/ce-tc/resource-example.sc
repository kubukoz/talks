def mkResource(s: String) = {
  val acquire = IO(println(s"Acquiring $s")) *> IO.pure(s)

  def release(s: String) = IO(println(s"Releasing $s"))

  Resource.make(acquire)(release)
}

val r = for {
  outer <- mkResource("outer")
  inner <- mkResource("inner")
} yield (outer, inner)

r.use { case (a, b) => IO(println(s"Using $a and $b")) }.unsafeRunSync
//Acquiring outer
//Acquiring inner
//Using outer and inner
//Releasing inner
//Releasing outer
