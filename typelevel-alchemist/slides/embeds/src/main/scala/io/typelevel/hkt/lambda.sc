def f[F[_]]: Int = 0

f[({ type L[A] = Either[String, A] })#L]

//equivalent to

type L[A] = Either[String, A]
f[L]

//kind-projector syntax
f[Either[String, ?]]
f[λ[A => Either[String, A]]]
f[Lambda[A => Either[String, A]]]
