import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.dom.*

import scala.scalajs.js.JSConverters.*

object NoteEditor {

  def show(
    editedNoteRef: Signal[IO, (Int, Int)],
    trackState: TrackState,
  ): Signal[IO, Resource[IO, HtmlDivElement[IO]]] = editedNoteRef.map {
    case (editedTrack, editedNote) =>
      div(
        "edited note: ",
        trackState.read.map(_(editedTrack)(editedNote).toString()),
        button(
          "clear",
          onClick --> {
            _.foreach { _ =>
              trackState.updateAtAndGet(editedTrack, editedNote)(_ => Playable.Rest).void
            }
          },
          styleAttr <-- trackState
            .read
            .map(_(editedTrack)(editedNote).ifRest("display: none")),
        ),
        button(
          "Play C4",
          onClick --> {
            _.foreach { _ =>
              trackState.updateAtAndGet(editedTrack, editedNote)(_ => Playable.C4).void
            }
          },
          styleAttr <-- trackState
            .read
            .map(_(editedTrack)(editedNote).ifPlay(_ => "display: none")),
        ),
        button(
          "pitch up",
          onClick --> {
            _.foreach { _ =>
              trackState.updateAtAndGet(editedTrack, editedNote)(_ + 1).void
            }
          },
          styleAttr <-- trackState
            .read
            .map(_(editedTrack)(editedNote).ifRest("display: none")),
        ),
        button(
          "pitch down",
          onClick --> {
            _.foreach { _ =>
              trackState.updateAtAndGet(editedTrack, editedNote)(_ - 1).void
            }
          },
          styleAttr <-- trackState
            .read
            .map(_(editedTrack)(editedNote).ifRest("display: none")),
        ),
      )
  }

}
