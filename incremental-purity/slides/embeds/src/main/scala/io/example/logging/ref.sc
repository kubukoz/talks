import cats.effect.IO
import cats.effect.concurrent.Ref

val refF: IO[Ref[IO, Int]] = Ref[IO].of[Int](0)
<span class="fragment">
val prog1: IO[Int] =
  refF.flatMap(_.update(_ + 1)) *>
    refF.flatMap(_.get)

<span class="fragment">prog1.unsafeRunSync() //0</span>
</span><span class="fragment">
val prog2: IO[Int] = refF.flatMap { ref =>
  ref.update(_ + 1) *>
    ref.get
}

<span class="fragment">prog2.unsafeRunSync() //1</span>
</span>
<span class="fragment">
//same as prog2
val prog3 = for {
  ref    <- refF
  _      <- ref.update(_ + 1)
  result <- ref.get
} yield result //1</span>
