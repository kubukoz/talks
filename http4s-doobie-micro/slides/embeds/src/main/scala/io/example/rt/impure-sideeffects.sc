import scala.io.StdIn

val x = StdIn.readLine()
//< Foo

val L = (x, x)
//L = (Foo, Foo)

val R = (StdIn.readLine, StdIn.readLine)
//< Foo
//< Bar
//R = (Foo, Bar)

L <!-> R
