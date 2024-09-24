import monocle.syntax.all.*
import cats.derived.*
import cats.kernel.Hash

enum Playable derives Hash {
  case Play(noteId: Int, velocity: Int)
  case Rest

  def atVelocity(velocity: Int): Playable = mapPlay(_.focus(_.velocity).set(velocity))
  def +(semitones: Int): Playable = mapPlay(_.focus(_.noteId).modify(_ + semitones))

  def mapPlay(f: Play => Play): Playable =
    this match {
      case p: Play => f(p)
      case _       => this
    }

}

object Playable {

  // max velocity by default

  def play(noteId: Int): Playable.Play = Playable.Play(noteId, 0x7f)
}
