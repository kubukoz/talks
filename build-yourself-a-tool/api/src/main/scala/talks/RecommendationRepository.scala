package talks

import cats.effect.IO
import cats.effect.Ref
import talks.api.PlaylistId
import talks.api.TalkId

trait RecommendationRepository {
  def create(playlistId: PlaylistId, talks: List[TalkId]): IO[Unit]
  def get(playlistId: PlaylistId): IO[Option[List[TalkId]]]
  def append(playlistId: PlaylistId, talk: TalkId): IO[Unit]
}

object RecommendationRepository {

  val inMemory: IO[RecommendationRepository] = Ref[IO].of(Map.empty[PlaylistId, List[TalkId]]).map {
    ref =>
      new RecommendationRepository {

        def create(playlistId: PlaylistId, talks: List[TalkId]): IO[Unit] = ref.update(
          _.updated(playlistId, talks)
        )

        def get(playlistId: PlaylistId): IO[Option[List[TalkId]]] = ref.get.map(_.get(playlistId))

        def append(playlistId: PlaylistId, talk: TalkId): IO[Unit] = ref.update(
          _.updatedWith(playlistId)(_.map(_ :+ talk))
        )

      }
  }

}
