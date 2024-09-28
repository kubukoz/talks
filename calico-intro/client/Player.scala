import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal

import scala.concurrent.duration.*

object Player {

  def run(
    instrument: Instrument,
    trackState: TrackState,
    midiChannel: Ref[IO, Int],
    holdAtRef: Ref[IO, Option[Int]],
    currentNoteRef: Ref[IO, Int],
    playingRef: Signal[IO, Boolean],
    transposeRef: Ref[IO, Int],
  ): Resource[IO, Unit] = {
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
                        instrument.send(MIDI.NoteOn(channel, noteId, velocity))
                      ) *>
                        instrument.send(MIDI.NoteOff(channel, noteId, 0)).delayBy(period / 4)
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
