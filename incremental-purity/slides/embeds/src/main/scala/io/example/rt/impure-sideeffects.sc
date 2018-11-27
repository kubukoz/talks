import scala.io.StdIn

val x = StdIn.readLine()
<span class="fragment">//&lt; Foo</span>

val L = (x, x)
<span class="fragment">//L = (Foo, Foo)</span>

val R = (StdIn.readLine, StdIn.readLine)
<span class="fragment">//&lt; Foo</span>
<span class="fragment">//&lt; Bar</span>
<span class="fragment">//R = (Foo, Bar)</span>

L <span class="fragment">!=</span> R
