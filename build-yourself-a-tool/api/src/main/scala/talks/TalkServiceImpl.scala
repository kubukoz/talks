package talks

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import talks.api.CreatePlaylistOutput
import talks.api.ExtendPlaylistOutput
import talks.api.PlaylistId
import talks.api.PlaylistNotFound
import talks.api.TalkService

object TalkServiceImpl {

  private val playlistSize = 5

  def instance(
    using generator: PlaylistGenerator,
    talks: TalkRepository,
    rules: RuleRepository,
    recommendations: RecommendationRepository,
  ): TalkService[IO] = new TalkService[IO] {

    def createPlaylist(): IO[CreatePlaylistOutput] = for {
      playlistId <- UUIDGen.randomUUID[IO].map(PlaylistId(_))
      candidates <- talks.getAll
      config <- rules.getAll
      picked <- generator.generate(config, candidates, history = Nil, count = playlistSize)
      _ <- recommendations.create(playlistId, picked.map(_.id))
    } yield CreatePlaylistOutput(playlistId, picked)

    def extendPlaylist(playlistId: PlaylistId): IO[ExtendPlaylistOutput] = for {
      historyIds <- recommendations
        .get(playlistId)
        .flatMap(IO.fromOption(_)(PlaylistNotFound(playlistId)))
      candidates <- talks.getAll
      config <- rules.getAll
      byId = candidates.map(talk => talk.id -> talk).toMap
      picked <- generator.generate(config, candidates, historyIds.flatMap(byId.get), count = 1)
      _ <- picked.traverse_(talk => recommendations.append(playlistId, talk.id))
    } yield ExtendPlaylistOutput(picked.headOption)

  }

}
