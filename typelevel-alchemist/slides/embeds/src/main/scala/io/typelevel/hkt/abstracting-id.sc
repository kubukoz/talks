def g[F[_]](ints: F[Int]): F[Int] = ints

type Id[A] = A
g[Id](5) //compiles now - 5 is treated as Id[Int]
