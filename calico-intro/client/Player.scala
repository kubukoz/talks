import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*
import scala.concurrent.duration.*

object Player {

  def run(
    trackState: TrackState,
    midiChannel: Ref[IO, Int],
    holdAtRef: Ref[IO, Option[Int]],
    currentNoteRef: Ref[IO, Int],
    playingRef: Signal[IO, Boolean],
    transposeRef: Ref[IO, Int],
  ): Resource[IO, Unit] = Window[IO]
    .navigator
    .requestMIDIAccess
    .flatMap(_.outputs)
    .map(_.values.head)
    .toResource
    .flatMap { output =>
      val period = 1.minute / 120 / 4
      fs2
        .Stream
        .fixedRateStartImmediately[IO](period)
        .zipRight(fs2.Stream.emits(0 until stepCount).repeat)
        .pauseWhen(playingRef.map(!_))
        .evalTap(currentNoteRef.set)
        .foreach { noteIndex =>
          (midiChannel.get, holdAtRef.get, transposeRef.get, trackState.read.get)
            .flatMapN { (channel, holdAt, transpose, tracks) =>
              // we play notes of each track in parallel
              // because their Off messages need to be sent at roughly the same time
              // and we can't wait for one note to finish before starting the next
              tracks
                .parTraverse_ { track =>
                  (track(holdAt.getOrElse(noteIndex)) + transpose) match {
                    case Playable.Play(noteId, velocity) =>
                      IO.uncancelable { poll =>
                        // playing is cancelable, stopping isn't.
                        // (browsers ignore this anyway though)
                        poll(
                          output.send(MIDI.NoteOn(channel, noteId, velocity).toArray)
                        ) *>
                          IO.sleep(period / 4) *>
                          output.send(MIDI.NoteOff(channel, noteId, 0).toArray)
                      }
                    case Playable.Rest => IO.unit
                  }
                }
            }
        }
        .compile
        .drain
        .background
        .void
    }

}
