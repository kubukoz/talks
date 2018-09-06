
def f1[A]: (A, A) => A

def f2[A]: (A, A) => (A, A)

def f3[A : Monoid]: (A, A) => A

def f4[A, B]: (A, B) => A

def f5: Boolean => Boolean

def f6: Option[Boolean] => Boolean

def f7: Boolean => Option[Boolean]

def f8: (Boolean, Boolean, Boolean) => Unit

def f9: Unit => Boolean

def receive: Any => Unit

def f10: Nothing => String

def f11: String => Nothing
