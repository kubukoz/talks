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
    recordingTrackRef: SignallingRef[IO, Int],
    recordingRef: SignallingRef[IO, Boolean],
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
              (holdAt.getOrElse(i) + 1).show
            },
            styleAttr <-- currentNoteRef.map { current =>
              if i == current then "color: red".some
              else
                none
            },
          )
        }.toList,
        th(
          "Rec",
          input.withSelf { self =>
            (
              `type` := "checkbox",
              checked <-- recordingRef,
              onChange {
                self.checked.get.flatMap(recordingRef.set)
              },
            )
          },
        ),
        th(""),
      ),
      styleAttr := """
                     |border: 2px solid black
                     |""".stripMargin,
    ),
    tbody(
      List.tabulate(trackCount) { trackIndex =>
        tr(
          td(
            ""
          ),
          List.tabulate(stepCount) { noteIndex =>
            td(
              styleAttr := """
                             |border: 2px solid black
                             |""".stripMargin,
              (holdAtRef, trackState.read)
                .mapN {
                  case (None, tracks)         => tracks(trackIndex)(noteIndex)
                  case (Some(holdAt), tracks) => tracks(trackIndex)(holdAt)
                }
                .map {
                  case Playable.Rest                   => "_"
                  case Playable.Play(noteId, velocity) => "x"
                }
                .changes,
              input.withSelf { self =>
                (
                  `type` := "radio",
                  nameAttr := "edit-playable",
                  value := show"$trackIndex,$noteIndex",
                  checked <-- editedNoteRef.map { case (editedTrack, editedNote) =>
                    editedTrack == trackIndex && editedNote == noteIndex
                  },
                  onChange {
                    self.value.get.flatMap {
                      case s"$trackIndex,$noteIndex" =>
                        editedNoteRef.set((trackIndex.toInt, noteIndex.toInt))
                      case _ => IO.raiseError(new Exception("invalid value of edit-playable"))
                    }
                  },
                )
              },
            )
          },
          td(
            input.withSelf { self =>
              (
                `type` := "radio",
                nameAttr := "recording-track",
                value := trackIndex.toString,
                checked <-- recordingTrackRef.map(_ == trackIndex),
                onChange {
                  self.value.get.flatMap { value =>
                    recordingTrackRef.set(value.toInt)
                  }
                },
                disabled <-- recordingRef.map(!_),
              )
            }
          ),
          td(
            button(
              "clear",
              onClick {
                trackState.clear(trackIndex)
              },
            )
          ),
        )
      }
    ),
  )

}
