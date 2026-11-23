package talks

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Random
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.resourceServiceBuilder
import smithy4s.http4s.SimpleRestJsonBuilder

object Main extends IOApp.Simple {

  def run: IO[Unit] = {
    for {
      talks <- Catalog.load.toResource
      given Random[IO] <- Random.scalaUtilRandom[IO].toResource
      api <- SimpleRestJsonBuilder.routes(Playlists.instance(talks)).resource
      _ <- EmberServerBuilder
        .default[IO]
        .withHttpApp(
          Router(
            "/api/thumbnails" -> resourceServiceBuilder[IO]("/").toRoutes,
            "/" -> api,
          ).orNotFound
        )
        .build
    } yield ()
  }.useForever

}
