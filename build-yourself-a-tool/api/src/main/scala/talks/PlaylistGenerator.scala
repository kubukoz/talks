package talks

import cats.effect.IO
import cats.syntax.all.*
import talks.api.GenerationStep
import talks.api.Rule
import talks.api.Talk

trait PlaylistGenerator {
  def generate(
    config: List[Rule],
    candidates: List[Talk],
    history: List[Talk],
    count: Int,
    randomizer: Randomizer,
  ): IO[List[GenerationStep]]
}

object PlaylistGenerator {

  private final case class State(history: List[Talk], steps: List[GenerationStep], remaining: Int)

  def instance(using engine: RuleEngine): PlaylistGenerator = new PlaylistGenerator {

    def generate(
      config: List[Rule],
      candidates: List[Talk],
      history: List[Talk],
      count: Int,
      randomizer: Randomizer,
    ): IO[List[GenerationStep]] = State(history, steps = Nil, remaining = count).tailRecM { state =>
      if (state.remaining <= 0) {
        IO.pure(state.steps.reverse.asRight[State])
      } else {
        engine
          .evaluate(config, candidates, state.history)
          .flatMap { evaluation =>
            val pick = evaluation.matching match {
              case Nil => IO.pure(none[Talk])
              case matching => randomizer.elementOf(matching).map(_.some)
            }

            pick.tupleLeft(evaluation)
          }
          .timed
          .map { case (elapsed, (evaluation, talk)) =>
            val step = GenerationStep(
              position = state.history.size + 1,
              evaluation = evaluation,
              durationNanos = elapsed.toNanos,
              talk = talk,
            )

            talk match {
              case None => (step :: state.steps).reverse.asRight[State]
              case Some(next) => State(state.history :+ next, step :: state.steps, state.remaining - 1).asLeft[List[GenerationStep]]
            }
          }
      }
    }

  }

}
