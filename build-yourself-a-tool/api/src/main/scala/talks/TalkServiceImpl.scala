package talks

import cats.effect.IO
import cats.effect.std.Random
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import talks.api.CreatePlaylistOutput
import talks.api.ExtendPlaylistOutput
import talks.api.PlaylistId
import talks.api.PlaylistNotFound
import talks.api.Talk
import talks.api.TalkId
import talks.api.TalkService

object TalkServiceImpl {

  private val playlistSize = 5

  def instance(talks: List[Talk])(using random: Random[IO], recommendations: RecommendationRepository): TalkService[IO] =
    new TalkService[IO] {

      private def pick(excluded: Set[TalkId], count: Int): IO[List[Talk]] = random
        .shuffleList(talks.filterNot(talk => excluded(talk.id)))
        .map(_.take(count))

      def createPlaylist(): IO[CreatePlaylistOutput] = for {
        playlistId <- UUIDGen.randomUUID[IO].map(PlaylistId(_))
        picked <- pick(Set.empty, playlistSize)
        _ <- recommendations.create(playlistId, picked.map(_.id))
      } yield CreatePlaylistOutput(playlistId, picked)

      def extendPlaylist(playlistId: PlaylistId): IO[ExtendPlaylistOutput] = for {
        existing <- recommendations
          .get(playlistId)
          .flatMap(IO.fromOption(_)(PlaylistNotFound(playlistId)))
        picked <- pick(existing.toSet, 1)
        _ <- picked.traverse_(talk => recommendations.append(playlistId, talk.id))
      } yield ExtendPlaylistOutput(picked.headOption)

    }

}
