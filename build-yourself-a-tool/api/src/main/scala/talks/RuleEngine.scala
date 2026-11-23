package talks

import talks.api.Every
import talks.api.Positions
import talks.api.Rule
import talks.api.Talk
import talks.api.TalkPredicate

trait RuleEngine {
  def matching(rules: List[Rule], candidates: List[Talk], history: List[Talk]): List[Talk]
}

object RuleEngine {

  val instance: RuleEngine = new RuleEngine {

    def matching(rules: List[Rule], candidates: List[Talk], history: List[Talk]): List[Talk] = {
      val position = history.size + 1

      rules.foldLeft(candidates) { (remaining, rule) =>
        val filtered = applyRule(rule, remaining, history, position)

        if (filtered.isEmpty && rule.skippable) remaining else filtered
      }
    }

  }

  private def applyRule(rule: Rule, candidates: List[Talk], history: List[Talk], position: Int): List[Talk] =
    rule match {
      case Rule.NoRepeats(_) =>
        val seen = history.map(_.id).toSet
        candidates.filterNot(talk => seen(talk.id))

      case Rule.Requirement(at, predicate, _) =>
        if (appliesAt(at, position)) candidates.filter(satisfies(predicate, _)) else candidates
    }

  private def appliesAt(positions: Positions, position: Int): Boolean = positions match {
    case Positions.EveryCase(Every(n)) => position % n == 0
    case Positions.AllCase             => true
  }

  private def satisfies(predicate: TalkPredicate, talk: Talk): Boolean = predicate match {
    case TalkPredicate.SpeakerCountryCase(country) => talk.speakers.exists(_.country.contains(country))
    case TalkPredicate.HasTagCase(tag)             => talk.tags.contains(tag)
    case TalkPredicate.ConferenceCase(conference)  => talk.conference == conference
    case TalkPredicate.MaxDurationSecondsCase(max) => talk.durationSeconds <= max
  }

}
