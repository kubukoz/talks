//0
def f1[A, B, C]: (A => B) => B => A => C
//1
def f2[A, B, C]: (A => C) => B => A => C
//1 (called andThen)
def f3[A, B, C]: (A => B) => (B => C) => A => C
//2 (ignore one of A or B)
def f4[A, B, C]: (A => B) => B => A => B
