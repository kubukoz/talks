type F[X] = Int

type G[H[_]] = H[String]

type A = G[F]
type B = G[[a] => Either[Int, a]]