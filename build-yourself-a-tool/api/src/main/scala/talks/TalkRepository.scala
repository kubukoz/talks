package talks

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.readClassLoaderResource
import smithy4s.Blob
import smithy4s.json.Json
import talks.api.Talk
import talks.data.TalkRecord
import talks.data.TalkRecords

trait TalkRepository {
  def getAll: IO[List[Talk]]
}

object TalkRepository {

  private val conferences: List[(String, String)] = List(
    "scalar-2026" -> "Scalar 2026",
    "scala-days-2026" -> "Scala Days 2026",
  )

  val resources: IO[TalkRepository] = conferences
    .flatTraverse { case (dir, conference) =>
      readClassLoaderResource[IO](s"$dir/talks.json")
        .compile
        .to(Array)
        .flatMap(bytes => IO.fromEither(Json.read[TalkRecords](Blob(bytes))))
        .map(_.value.map(toTalk(dir, conference)))
    }
    .map { talks =>
      new TalkRepository {
        val getAll: IO[List[Talk]] = IO.pure(talks)
      }
    }

  private def toTalk(dir: String, conference: String)(record: TalkRecord): Talk = Talk(
    id = record.id,
    conference = conference,
    title = record.title,
    speakers = record.speakers,
    url = record.url,
    durationSeconds = record.durationSeconds,
    thumbnailUrl = s"/api/thumbnails/$dir/${record.thumbnail}",
    tags = record.tags,
    _abstract = record._abstract,
  )

}
