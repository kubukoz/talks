package talks

import cats.effect.IO
import talks.api.Rule

trait RuleRepository {
  def getAll: IO[List[Rule]]
}

object RuleRepository {

  def const(rules: List[Rule]): RuleRepository = new RuleRepository {
    val getAll: IO[List[Rule]] = IO.pure(rules)
  }

}
