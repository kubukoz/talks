def f[A]: Int = 1

f[Int]
f[Option] //doesn't compile - Option isn't a *-kinded type

import scala.language.higherKinds
def g[F[_]](ints: F[Int]): F[Int] = ints

g(Option(5)) //ok

//doesn't compile - Option[String] isn't an F[Int]
g(Option("oops"))

//doesn't compile - Int doesn't match the F[_] shape
g(5)
