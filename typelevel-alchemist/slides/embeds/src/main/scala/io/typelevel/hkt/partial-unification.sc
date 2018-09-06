
val a: Either[String, Int] = Right(6)

def g[F[_]](ints: F[Int]): F[Int] = ints

g(a) //F = Either[String, ?] inferred