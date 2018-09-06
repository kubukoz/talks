//2
def f1[A]: (A, A) => A
//2
def f2[A]: (A, A) => (A, A)
//infinite!
def f3[A : Monoid]: (A, A) => A
//1
def f4[A, B]: (A, B) => A
//4
def f5: Boolean => Boolean
//8
def f6: Option[Boolean] => Boolean
//9
def f7: Boolean => Option[Boolean]
//1
def f8: (Boolean, Boolean, Boolean) => Unit
//2
def f9: Unit => Boolean
//1
def receive: Any => Unit
//0 or 1 depending on whom you ask
def f10: Nothing => String
//impossible to implement
def f11: String => Nothing
