package talks

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import talks.api.CreatePlaylistOutput
import talks.api.ExtendPlaylistOutput
import talks.api.NoRepeats
import talks.api.PlaylistId
import talks.api.PlaylistNotFound
import talks.api.Rule
import talks.api.Talk
import talks.api.TalkService

object TalkServiceImpl {

  private val playlistSize = 5
  private val rules: List[Rule] = List(Rule.noRepeats(NoRepeats()))

  def instance(
    talks: List[Talk]
  )(
    using generator: PlaylistGenerator,
    recommendations: RecommendationRepository,
  ): TalkService[IO] = new TalkService[IO] {

    private val talksById = talks.map(talk => talk.id -> talk).toMap

    def createPlaylist(): IO[CreatePlaylistOutput] = for {
      playlistId <- UUIDGen.randomUUID[IO].map(PlaylistId(_))
      picked <- generator.generate(rules, history = Nil, count = playlistSize)
      _ <- recommendations.create(playlistId, picked.map(_.id))
    } yield CreatePlaylistOutput(playlistId, picked)

    def extendPlaylist(playlistId: PlaylistId): IO[ExtendPlaylistOutput] = for {
      history <- recommendations
        .get(playlistId)
        .flatMap(IO.fromOption(_)(PlaylistNotFound(playlistId)))
      picked <- generator.generate(rules, history.flatMap(talksById.get), count = 1)
      _ <- picked.traverse_(talk => recommendations.append(playlistId, talk.id))
    } yield ExtendPlaylistOutput(picked.headOption)

  }

}
