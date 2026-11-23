package talks

import talks.api.Rule
import talks.api.Talk

trait RuleEngine {
  def matching(rules: List[Rule], history: List[Talk]): List[Talk]
}

object RuleEngine {

  def instance(talks: List[Talk]): RuleEngine = new RuleEngine {

    def matching(rules: List[Rule], history: List[Talk]): List[Talk] = rules.foldLeft(talks) {
      case (candidates, Rule.NoRepeatsCase(_)) =>
        val seen = history.map(_.id).toSet
        candidates.filterNot(talk => seen(talk.id))
    }

  }

}
