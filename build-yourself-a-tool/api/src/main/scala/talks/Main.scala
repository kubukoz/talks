package talks

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Random
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.resourceServiceBuilder
import smithy4s.http4s.SimpleRestJsonBuilder
import talks.api.NoRepeats
import talks.api.Rule

object Main extends IOApp.Simple {

  def run: IO[Unit] = {
    for {
      given TalkRepository <- TalkRepository.resources.toResource
      given RuleRepository = RuleRepository.const(List(Rule.noRepeats(NoRepeats())))
      given RecommendationRepository <- RecommendationRepository.inMemory.toResource
      given Random[IO] <- Random.scalaUtilRandom[IO].toResource
      given RuleEngine = RuleEngine.instance
      given PlaylistGenerator = PlaylistGenerator.random
      api <- SimpleRestJsonBuilder.routes(TalkServiceImpl.instance).resource
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
