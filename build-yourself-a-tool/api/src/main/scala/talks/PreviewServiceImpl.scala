package talks

import cats.effect.IO
import cats.syntax.all.*
import talks.api.GetDefaultRulesOutput
import talks.api.PreviewOutput
import talks.api.PreviewService
import talks.api.Rule
import talks.api.TalkId
import talks.api.UnknownTalk

object PreviewServiceImpl {

  def instance(
    using generator: PlaylistGenerator,
    talks: TalkRepository,
    rules: RuleRepository,
  ): PreviewService[IO] =
    new PreviewService[IO] {

      def getDefaultRules(): IO[GetDefaultRulesOutput] = rules.getAll.map(GetDefaultRulesOutput(_))

      def preview(rules: List[Rule], seed: Long, count: Int, history: List[TalkId]): IO[PreviewOutput] = for {
        candidates <- talks.getAll
        byId = candidates.map(talk => talk.id -> talk).toMap
        historyTalks <- history.traverse(id => IO.fromOption(byId.get(id))(UnknownTalk(id)))
        randomizer <- Randomizer.seeded(seed)
        trace <- generator.generate(rules, candidates, historyTalks, count, randomizer)
      } yield PreviewOutput(trace)

    }

}
