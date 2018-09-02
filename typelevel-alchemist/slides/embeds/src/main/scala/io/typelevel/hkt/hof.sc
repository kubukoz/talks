//String => Int function
def f(x: String): Int = 5

//HOF taking String => Int
def g(f: String => Int)

val a = g(f)
val b = g(_.length)

//higher-kinded type F
type F[X] = Int

//higher-order type with higher-kinded type parameter H
type G[H[_]] = H[Int]

//*-kinded type A = G[F] = F[Int] = Int
type A = G[F]

//*-kinded type B = G[Option] = Option[Int]
type B = G[Option]
