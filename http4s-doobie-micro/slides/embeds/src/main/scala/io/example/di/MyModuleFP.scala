package modull

import cats.effect._
import cats.Applicative
import cats.implicits._

//stubs
trait UserRepository[F[_]]
object UserRepository {
  def make[F[_]]: UserRepository[F] = new UserRepository[F]{}
}
object DataSource {
  def make[F[_]: Applicative]: Resource[F, Unit] = Resource.pure[F, Unit](())
  type Module = Unit
}
import DataSource.Module

//actual code
object MyModule {
  def make: Resource[IO, Module] =
    DataSource.make[IO].map { implicit ds =>
        implicit val userRepository: UserRepository[IO] =
            UserRepository.make
        //...
    }
}
