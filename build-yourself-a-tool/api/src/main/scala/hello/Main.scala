package hello

import cats.effect.IO
import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import smithy4s.http4s.SimpleRestJsonBuilder

object Main extends IOApp.Simple {

  private val service: HelloService[IO] = new HelloService[IO] {
    def hello(name: String): IO[HelloOutput] = IO.pure(HelloOutput(s"Hello, $name!"))
  }

  def run: IO[Unit] = SimpleRestJsonBuilder
    .routes(service)
    .resource
    .flatMap { routes =>
      EmberServerBuilder.default[IO].withHttpApp(routes.orNotFound).build
    }
    .useForever

}
