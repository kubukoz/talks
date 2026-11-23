package talks

import talks.api.Rule
import talks.api.Talk

trait RuleEngine {
  def matching(rules: List[Rule], candidates: List[Talk], history: List[Talk]): List[Talk]
}

object RuleEngine {

  val instance: RuleEngine = new RuleEngine {

    def matching(rules: List[Rule], candidates: List[Talk], history: List[Talk]): List[Talk] =
      rules.foldLeft(candidates) { case (remaining, Rule.NoRepeatsCase(_)) =>
        val seen = history.map(_.id).toSet
        remaining.filterNot(talk => seen(talk.id))
      }

  }

}
