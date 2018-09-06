val headTrans: List ~> Option = λ[List ~> Option](_.headOption)

//apply like a normal function
val applied = headTrans(List(1,2,3)) //Some(1)
