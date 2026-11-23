package talks

import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.resourceServiceBuilder
import smithy4s.http4s.SimpleRestJsonBuilder
import talks.api.CountryCode
import talks.api.Every
import talks.api.Positions
import talks.api.Rule
import talks.api.TalkPredicate

object Main extends IOApp.Simple {

  def run: IO[Unit] = {
    for {
      given TalkRepository <- TalkRepository.resources.toResource
      given RuleRepository = RuleRepository.const(
        List(
          Rule.NoRepeats(),
          Rule.Requirement(Positions.all(), TalkPredicate.maxDurationSeconds(30 * 60)),
          Rule.Requirement(Positions.every(Every(2)), TalkPredicate.hasTag("ai")),
          Rule.Spacing(3, TalkPredicate.speakerCountry(CountryCode("PL")), skippable = true),
        )
      )
      given RecommendationRepository <- RecommendationRepository.inMemory.toResource
      given Randomizer <- Randomizer.secure.toResource
      given RuleEngine = RuleEngine.instance
      given PlaylistGenerator = PlaylistGenerator.instance
      talkRoutes <- SimpleRestJsonBuilder.routes(TalkServiceImpl.instance).resource
      previewRoutes <- SimpleRestJsonBuilder.routes(PreviewServiceImpl.instance).resource
      _ <- EmberServerBuilder
        .default[IO]
        .withHttpApp(
          Router(
            "/api/thumbnails" -> resourceServiceBuilder[IO]("/").toRoutes,
            "/" -> (talkRoutes <+> previewRoutes),
          ).orNotFound
        )
        .build
    } yield ()
  }.useForever

}
