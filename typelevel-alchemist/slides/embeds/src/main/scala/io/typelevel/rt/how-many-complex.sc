def f1[A, B, C]: (A => B) => B => A => C

def f2[A, B, C]: (A => C) => B => A => C

def f3[A, B, C]: (A => B) => (B => C) => A => C

def f4[A, B, C]: (A => B) => B => A => B
