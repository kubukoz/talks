package talks

import cats.effect.IO
import cats.effect.std.Random
import cats.effect.std.SecureRandom

trait Randomizer {
  def elementOf[A](values: Iterable[A]): IO[A]
}

object Randomizer {

  def seeded(seed: Long): IO[Randomizer] = Random.scalaUtilRandomSeedLong[IO](seed).map(fromRandom)

  val secure: IO[Randomizer] = SecureRandom.javaSecuritySecureRandom[IO].map(fromRandom)

  private def fromRandom(random: Random[IO]): Randomizer =
    new Randomizer {
      def elementOf[A](values: Iterable[A]): IO[A] = random.elementOf(values)
    }

}
