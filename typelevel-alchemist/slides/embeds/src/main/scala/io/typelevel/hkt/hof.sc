def f(x: String): Int = 5

def g(f: String => Int) = f("a")

val a = g(f)
val b = g(a => a.length)