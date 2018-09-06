import cats.effect.IO

//don't do this, use Ref
object mutable {
  private var a = 0

  val incGet: IO[Int] = IO { a += 1; a }
}
