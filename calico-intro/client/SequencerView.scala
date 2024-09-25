import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.*
import org.http4s.implicits.*

object SequencerView {

  def show(
    trackState: TrackState,
    currentNoteRef: Signal[IO, Int],
    holdAtRef: Signal[IO, Option[Int]],
    editedNoteRef: SignallingRef[IO, (Int, Int)],
  ): Resource[IO, HtmlTableElement[IO]] = table(
    styleAttr := """
                   |border: 2px solid black
                   |""".stripMargin,
    thead(
      tr(
        th("·"),
        (0 until stepCount).map { i =>
          th(
            holdAtRef.map { holdAt =>
              holdAt.getOrElse(i).show
            },
            styleAttr <-- currentNoteRef.map { current =>
              if i == current then "color: red"
              else
                ""
            },
          )
        }.toList,
      ),
      styleAttr := """
                     |border: 2px solid black
                     |""".stripMargin,
    ),
    tbody(
      children[(List[Playable], Int)] { (track, trackIndex) =>
        tr(
          td(
            ""
          ),
          track.zipWithIndex.map { (playable, noteIndex) =>
            td(
              styleAttr := """
                             |border: 2px solid black
                             |""".stripMargin,
              (holdAtRef, trackState.read)
                .mapN {
                  case (None, _)              => playable
                  case (Some(holdAt), tracks) => tracks(trackIndex)(holdAt)
                }
                .map {
                  case Playable.Rest                   => "_"
                  case Playable.Play(noteId, velocity) => "x"
                },
              input.withSelf { self =>
                (
                  `type` := "radio",
                  nameAttr := "edit-playable",
                  value := show"$trackIndex,$noteIndex",
                  checked <-- editedNoteRef.map { case (editedTrack, editedNote) =>
                    editedTrack == trackIndex && editedNote == noteIndex
                  },
                  onChange --> {
                    _.foreach { _ =>
                      self.value.get.flatMap {
                        case s"$trackIndex,$noteIndex" =>
                          editedNoteRef.set((trackIndex.toInt, noteIndex.toInt))
                        case _ => IO.raiseError(new Exception("invalid value of edit-playable"))
                      }
                    }
                  },
                )
              },
            )
          },
        )
      } <-- trackState.read.map(_.zipWithIndex)
    ),
  )

}
