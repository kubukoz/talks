package talks

import cats.effect.IO
import cats.effect.std.Random
import talks.api.ExtendPlaylistOutput
import talks.api.Talk
import talks.api.TalkId
import talks.api.TalkService

object Playlists {

  def instance(talks: List[Talk])(using Random[IO]): TalkService[IO] = new TalkService[IO] {

    def extendPlaylist(playlist: List[TalkId], count: Int): IO[ExtendPlaylistOutput] = {
      val included = playlist.toSet

      Random[IO]
        .shuffleList(talks.filterNot(talk => included(talk.id)))
        .map(candidates => ExtendPlaylistOutput(candidates.take(count)))
    }

  }

}
