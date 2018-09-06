import scala.util.Try

def break(): Nothing = throw new RuntimeException

//v1 - ok
Try(break())

//v2
val a = break() //throws here, we never get to the next line
Try(a)
