trait FunctionK[F[_], G[_]] {

  /**
    * Applies this functor transformation from `F` to `G`
    */
  def apply[A](fa: F[A]): G[A]
}

//provided by cats
type ~>[F[_], G[_]] = FunctionK[F, G]

//sample natural transformation
val headTrans: List ~> Option = λ[List ~> Option](_.headOption)

//apply like a normal function
val applied = headTrans(List(1,2,3)) //Some(1)
