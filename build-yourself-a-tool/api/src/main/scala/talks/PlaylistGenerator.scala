package talks

import cats.effect.IO
import cats.effect.std.Random
import cats.syntax.all.*
import talks.api.Rule
import talks.api.Talk

trait PlaylistGenerator {
  def generate(config: List[Rule], candidates: List[Talk], history: List[Talk], count: Int): IO[List[Talk]]
}

object PlaylistGenerator {

  private final case class State(history: List[Talk], picked: List[Talk], remaining: Int)

  def random(using engine: RuleEngine, random: Random[IO]): PlaylistGenerator = new PlaylistGenerator {

    def generate(config: List[Rule], candidates: List[Talk], history: List[Talk], count: Int): IO[List[Talk]] =
      State(history, picked = Nil, remaining = count).tailRecM { state =>
        val done = IO.pure(state.picked.reverse.asRight[State])

        if (state.remaining <= 0) {
          done
        } else {
          engine.matching(config, candidates, state.history) match {
            case Nil => done
            case matching =>
              random.elementOf(matching).map { talk =>
                State(state.history :+ talk, talk :: state.picked, state.remaining - 1).asLeft[List[Talk]]
              }
          }
        }
      }

  }

}
