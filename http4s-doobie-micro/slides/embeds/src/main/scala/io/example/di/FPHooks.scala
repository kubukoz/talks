import cats.effect._
import cats.implicits._

//stubs
trait Config 
trait HikariDataSource {
  def close: IO[Unit]
}
class DataSource(underlying: HikariDataSource)

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
      .map(new DataSource(_))
}
