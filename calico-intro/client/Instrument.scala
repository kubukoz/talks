import cats.effect.IO
import fs2.dom.*
import fs2.dom.ext.MIDIOutput
import org.soundsofscala.instrument.Default
import org.soundsofscala.instrument.Synth
import org.soundsofscala.instrument.Synth.Settings

trait Instrument {
  def play(note: Int, velocity: Int): IO[Unit]
  def stop(note: Int): IO[Unit]
}

object Instrument {

  def fromMidiOutput(output: MIDIOutput[IO], channel: IO[Int]): Instrument =
    new {

      def play(note: Int, velocity: Int): IO[Unit] = channel.flatMap { ch =>
        output.send(MIDI.NoteOn(ch, note, velocity).toArray)
      }

      def stop(note: Int): IO[Unit] = channel.flatMap { ch =>
        output.send(MIDI.NoteOff(ch, note, 0).toArray)
      }
    }

  def fromSos(): Instrument =
    new {
      import org.soundsofscala.syntax.all.*

      import cats.effect.IO
      import org.scalajs.dom
      import org.scalajs.dom.AudioContext
      import org.soundsofscala
      import org.soundsofscala.graph.AudioNode.*
      import org.soundsofscala.graph.AudioParam
      import org.soundsofscala.graph.AudioParam.AudioParamEvent.ExponentialRampToValueAtTime
      import org.soundsofscala.graph.AudioParam.AudioParamEvent.LinearRampToValueAtTime
      import org.soundsofscala.graph.AudioParam.AudioParamEvent.SetValueAtTime
      import org.soundsofscala.models
      import org.soundsofscala.models.AtomicMusicalEvent.Note
      import org.soundsofscala.models.*

      private def synth(
        using AudioContext
      ) =
        new Synth {
          def attackRelease(
            when: Double,
            note: Note,
            tempo: Tempo,
            attack: Attack,
            release: Release,
          ): IO[Unit] = IO {
            val synthVelocity = note.velocity.getNormalisedVelocity

            val filter = lowPassFilter.withFrequency(
              AudioParam(
                Vector(
                  ExponentialRampToValueAtTime(1000, when),
                  LinearRampToValueAtTime(10000, when + note.durationToSeconds(tempo)),
                )
              )
            )

            val sawGain = Gain(
              List.empty,
              AudioParam(
                Vector(
                  ExponentialRampToValueAtTime(synthVelocity / 4, when),
                  LinearRampToValueAtTime(0.000001, when + note.durationToSeconds(tempo)),
                )
              ),
            )

            val osc2Saw = sawtoothOscillator(when, note.durationToSeconds(tempo)).withFrequency(
              AudioParam(Vector(SetValueAtTime(note.frequency, when)))
            )

            val graph2 = osc2Saw --> filter --> sawGain
            graph2.create
          }
        }

      def play(note: Int, velocity: Int): IO[Unit] = {
        given audioContext: dom.AudioContext = new dom.AudioContext()

        val sosPitch: Octave => Note =
          (note % 12).match {
            case 0  => C
            case 1  => C(_).sharp
            case 2  => D
            case 3  => D(_).sharp
            case 4  => E
            case 5  => F
            case 6  => F(_).sharp
            case 7  => G
            case 8  => G(_).sharp
            case 9  => A
            case 10 => A(_).sharp
            case 11 => B
          }

        val sosNote = sosPitch(Octave.from((note / 12) - 1).toOption.get)

        synth.play(
          musicEvent = sosNote,
          when = 0,
          tempo = org.soundsofscala.models.Tempo(120),
        )(Default.default)
      }

      def stop(note: Int): IO[Unit] = IO.unit
    }

  def suspend(f: IO[Instrument]): Instrument =
    new {
      def play(note: Int, velocity: Int): IO[Unit] = f.flatMap(_.play(note, velocity))
      def stop(note: Int): IO[Unit] = f.flatMap(_.stop(note))
    }

}
