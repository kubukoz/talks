import cats.effect._
import cats.implicits._

//stubs
trait Config 
trait HikariDataSource {
  def close: IO[Unit]
  def immutable: ImmutableHikariDataSource = new ImmutableHikariDataSource
}

class DataSource(underlying: ImmutableHikariDataSource)

class ImmutableHikariDataSource
object Imports {
  def newHikariPool(config: Config): IO[HikariDataSource] = IO(new HikariDataSource {
    def close: IO[Unit] = IO.unit
  })
}

import Imports._

//actual code
object DataSource {
  def make(config: Config): Resource[IO, DataSource] =
    Resource.make(newHikariPool(config))(_.close)
      .map(_.immutable)
      .map(new DataSource(_))
}
