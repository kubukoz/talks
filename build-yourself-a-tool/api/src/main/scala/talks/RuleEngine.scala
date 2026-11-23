package talks

import cats.effect.IO
import cats.syntax.all.*
import talks.api.Evaluation
import talks.api.Every
import talks.api.Positions
import talks.api.Rule
import talks.api.RuleEvaluation
import talks.api.RuleOutcome
import talks.api.Talk
import talks.api.TalkPredicate

trait RuleEngine {
  def evaluate(rules: List[Rule], candidates: List[Talk], history: List[Talk]): IO[Evaluation]
  def satisfiesPredicate(rule: Rule, talk: Talk): Option[Boolean]
}

object RuleEngine {

  val instance: RuleEngine = new RuleEngine {

    def evaluate(rules: List[Rule], candidates: List[Talk], history: List[Talk]): IO[Evaluation] = {
      val position = history.size + 1

      rules
        .foldLeftM((candidates, List.empty[RuleEvaluation])) { case ((remaining, evaluations), rule) =>
          IO(applyRule(rule, remaining, history, position)).timed.map { case (elapsed, split) =>
            val (matched, rejected) = split.getOrElse((remaining, Nil))

            val outcome = split match {
              case None => RuleOutcome.INACTIVE
              case Some((Nil, _)) if rule.skippable => RuleOutcome.SKIPPED
              case Some(_) => RuleOutcome.APPLIED
            }

            val evaluation = RuleEvaluation(
              rule = rule,
              outcome = outcome,
              matchedBefore = remaining.size,
              matchedAfter = matched.size,
              rejected = rejected,
              durationNanos = elapsed.toNanos,
            )

            val next = if (outcome == RuleOutcome.SKIPPED) remaining else matched

            (next, evaluation :: evaluations)
          }
        }
        .map { case (matching, evaluations) => Evaluation(matching = matching, rules = evaluations.reverse) }
    }

    def satisfiesPredicate(rule: Rule, talk: Talk): Option[Boolean] = predicateOf(rule).map(satisfies(_, talk))

  }

  private def applyRule(
    rule: Rule,
    candidates: List[Talk],
    history: List[Talk],
    position: Int,
  ): Option[(List[Talk], List[Talk])] = rule match {
    case Rule.NoRepeats(_) =>
      val seen = history.map(_.id).toSet
      Some(candidates.partition(talk => !seen(talk.id)))

    case Rule.Requirement(at, predicate, _) =>
      required(at, position).map(expected => candidates.partition(satisfies(predicate, _) == expected))

    case Rule.Spacing(n, predicate, _) =>
      history.lastIndexWhere(satisfies(predicate, _)) match {
        case -1 => None
        case lastIndex =>
          val due = position - (lastIndex + 1) >= n
          Some(candidates.partition(talk => satisfies(predicate, talk) == due))
      }
  }

  private def required(positions: Positions, position: Int): Option[Boolean] = positions match {
    case Positions.EveryCase(Every(n)) if position % n == 0 => Some(true)
    case Positions.EveryCase(_) if position == 1            => None
    case Positions.EveryCase(_)                             => Some(false)
    case Positions.AllCase                                  => Some(true)
  }

  private def predicateOf(rule: Rule): Option[TalkPredicate] = rule match {
    case Rule.NoRepeats(_)                 => None
    case Rule.Requirement(_, predicate, _) => Some(predicate)
    case Rule.Spacing(_, predicate, _)     => Some(predicate)
  }

  private def satisfies(predicate: TalkPredicate, talk: Talk): Boolean = predicate match {
    case TalkPredicate.SpeakerCountryCase(country) => talk.speakers.exists(_.country.contains(country))
    case TalkPredicate.HasTagCase(tag)             => talk.tags.contains(tag)
    case TalkPredicate.ConferenceCase(conference)  => talk.conference == conference
    case TalkPredicate.MaxDurationSecondsCase(max) => talk.durationSeconds <= max
  }

}
