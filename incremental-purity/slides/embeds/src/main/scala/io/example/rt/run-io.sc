import cats.effect.IO
import cats.syntax.functor._

val foo: IO[Int] = IO(println("Foo!")).as(5)

<span class="fragment">foo.unsafeRunAsyncAndForget()</span>

<span class="fragment">foo.unsafeRunAsync {
  case Right(v) => log.info(s"Program completed with: $v")
  case Left(ex) => log.error("Program failed", ex)
}</span>

<span class="fragment">import scala.concurrent.duration._
//limits the duration of a single async block
val maybeResult: Option[Int] = foo.unsafeRunTimed(5.seconds)</span>

<span class="fragment">//good for tests
val result = foo.timeout(5.seconds).unsafeRunSync()</span>

<span class="fragment">foo.unsafeToFuture()</span>

<span class="fragment">foo.unsafeRunSync() //yolo mode</span>
