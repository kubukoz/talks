package talks

import cats.Monoid
import cats.Semigroup
import cats.data.Chain
import cats.data.Writer
import cats.effect.IO
import cats.syntax.all.*
import hearth.kindlings.catsderivation.extensions.*
import smithy4s.Document
import talks.api.Evaluation
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

  val instance: RuleEngine =
    new RuleEngine {

      def evaluate(rules: List[Rule], candidates: List[Talk], history: List[Talk])
        : IO[Evaluation] = {
        val position = history.size + 1

        rules
          .foldLeftM((candidates, List.empty[RuleEvaluation])) {
            case ((remaining, evaluations), rule) =>
              IO(applyRule(rule, remaining, history, position).run).timed.map {
                case (elapsed, (diagnostics, split)) =>
                  val (matched, rejected) = split.getOrElse((remaining, Nil))

                  val outcome =
                    split match {
                      case None                             => RuleOutcome.INACTIVE
                      case Some((Nil, _)) if rule.skippable => RuleOutcome.SKIPPED
                      case Some(_)                          => RuleOutcome.APPLIED
                    }

                  val evaluation = RuleEvaluation(
                    rule = rule,
                    outcome = outcome,
                    matchedBefore = remaining.size,
                    matchedAfter = matched.size,
                    rejected = rejected,
                    durationNanos = elapsed.toNanos,
                    logs = diagnostics.logs.toList,
                    context = diagnostics.context,
                  )

                  val next =
                    if (outcome == RuleOutcome.SKIPPED)
                      remaining
                    else
                      matched

                  (next, evaluation :: evaluations)
              }
          }
          .map { case (matching, evaluations) =>
            Evaluation(matching = matching, rules = evaluations.reverse)
          }
      }

      def satisfiesPredicate(rule: Rule, talk: Talk): Option[Boolean] = predicateOf(rule).map(
        satisfies(_, talk)
      )

    }

  private given Semigroup[Document] = Semigroup.last

  private final case class Diagnostics(logs: Chain[String], context: Map[String, Document])
    derives Monoid

  private type Logged[A] = Writer[Diagnostics, A]

  private def log(message: String): Logged[Unit] = Writer.tell(
    Diagnostics(Chain.one(message), Map.empty)
  )

  private def context(entries: (String, Document)*): Logged[Unit] = Writer.tell(
    Diagnostics(Chain.empty, entries.toMap)
  )

  private def number(value: Int): Document = Document.DNumber(BigDecimal(value))

  private def applyRule(
    rule: Rule,
    candidates: List[Talk],
    history: List[Talk],
    position: Int,
  ): Logged[Option[(List[Talk], List[Talk])]] = {
    def expect(predicate: TalkPredicate, expected: Boolean)
      : Logged[Option[(List[Talk], List[Talk])]] = {
      val (matching, other) = candidates.partition(satisfies(predicate, _))
      val (kept, rejected) =
        if (expected)
          (matching, other)
        else
          (other, matching)

      context(
        "mustSatisfy" -> Document.DBoolean(expected),
        "satisfyingCandidates" -> number(matching.size),
      ) *>
        log(s"Position $position needs a talk that ${describe(predicate, expected)}")
          .as((kept, rejected).some)
    }

    rule match {
      case Rule.NoRepeats(_) =>
        val seen = history.map(_.id).toSet
        val (fresh, repeats) = candidates.partition(talk => !seen(talk.id))
        context("alreadyPlayed" -> number(seen.size)).as((fresh, repeats).some)

      case Rule.Requirement(predicate, _) => expect(predicate, true)

      case Rule.Spacing(n, predicate, _) =>
        history.lastIndexWhere(satisfies(predicate, _)) match {
          case -1 =>
            log(
              s"No earlier talk ${describe(predicate, true)}, so spacing starts once one is played"
            )
              .as(None)
          case lastIndex =>
            val lastPosition = lastIndex + 1
            val distance = position - lastPosition
            val due = distance >= n

            context("lastMatchPosition" -> number(lastPosition), "distance" -> number(distance)) *>
              log(
                if (distance > n)
                  s"Overdue: last match at position $lastPosition is $distance back, more than $n"
                else if (due)
                  s"Due: last match at position $lastPosition is exactly $n back"
                else
                  s"Too soon: last match at position $lastPosition is only $distance back, needs $n"
              ) *>
              expect(predicate, due)
        }
    }
  }

  private def predicateOf(rule: Rule): Option[TalkPredicate] =
    rule match {
      case Rule.NoRepeats(_)              => None
      case Rule.Requirement(predicate, _) => Some(predicate)
      case Rule.Spacing(_, predicate, _)  => Some(predicate)
    }

  private def describe(predicate: TalkPredicate, positive: Boolean): String =
    predicate match {
      case TalkPredicate.SpeakerCountryCase(country) =>
        if (positive)
          s"has a speaker from $country"
        else
          s"has no speaker from $country"
      case TalkPredicate.HasTagCase(tag) =>
        if (positive)
          s"is tagged \"$tag\""
        else
          s"is not tagged \"$tag\""
      case TalkPredicate.ConferenceCase(conference) =>
        if (positive)
          s"is from $conference"
        else
          s"is not from $conference"
      case TalkPredicate.MaxDurationSecondsCase(max) =>
        if (positive)
          s"is at most ${max / 60} min"
        else
          s"is longer than ${max / 60} min"
    }

  private def satisfies(predicate: TalkPredicate, talk: Talk): Boolean =
    predicate match {
      case TalkPredicate.SpeakerCountryCase(country) =>
        talk.speakers.exists(_.country.contains(country))
      case TalkPredicate.HasTagCase(tag)             => talk.tags.contains(tag)
      case TalkPredicate.ConferenceCase(conference)  => talk.conference == conference
      case TalkPredicate.MaxDurationSecondsCase(max) => talk.durationSeconds <= max
    }

}
