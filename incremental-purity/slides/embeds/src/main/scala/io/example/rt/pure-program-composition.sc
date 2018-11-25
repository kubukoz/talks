val printWorld = IO(print("world"))
val printHello = IO(print("Hello"))
val printSpace = IO(print(" "))

val prog = printHello *> printSpace *> printWorld
<span class="fragment">
prog.unsafeRunSync()
//> Hello world
</span>
val prog2 = printWorld *> printSpace *> printWorld *> printHello
<span class="fragment">
prog2.unsafeRunSync()
//> world worldHello
</span>
