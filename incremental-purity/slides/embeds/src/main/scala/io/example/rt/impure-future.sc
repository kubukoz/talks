import scala.concurrent.Future
import scala.io.StdIn
import scala.concurrent.ExecutionContext.Implicits.global

val x = Future(StdIn.readLine())

(x, x) <span class="fragment">!=</span> (Future(StdIn.readLine()), Future(StdIn.readLine()))
