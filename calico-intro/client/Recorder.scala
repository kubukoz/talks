import cats.effect.IO
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.syntax.all.*

object Recorder {

  def run(
    instrument: Instrument,
    currentNoteRef: Ref[IO, Int],
    recordingRef: Ref[IO, Boolean],
    recordingTrackRef: Ref[IO, Int],
    trackState: TrackState,
  ): IO[Unit] = "awsedftgyhujkolp".toList.zipWithIndex.parTraverse_ { (key, i) =>
    KeyStatus
      .forKey(key.show)
      .changes
      .evalMap { isOn =>
        val noteToPlay = i + 60
        val velocity = 100

        val play =
          if isOn then instrument.play(noteToPlay, velocity)
          else
            instrument.stop(noteToPlay)

        val register = (currentNoteRef.get, recordingRef.get, recordingTrackRef.get).flatMapN {
          case (currentNote, true, track) =>
            trackState.update { tracks =>
              tracks
                .updated(
                  track,
                  tracks(track).updated(currentNote, Playable.Play(noteToPlay, velocity)),
                )
            }
          case _ => IO.unit
        }

        play *> register.whenA(isOn)
      }
      .compile
      .drain
  }

}
