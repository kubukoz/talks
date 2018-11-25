private def useKnownSkill[F[_]: Log: Functor](task: Task): F[FiniteDuration] = {
  val info = Log[F].putStrLn(s"I know how to do ${task.skill.name}! Adding only 1 second")

  info.as(1.second)
}
<span class="fragment">

private def solveAndLearnSkill[F[_]: HasSkillState: FlatMap: Log](task: Task): F[FiniteDuration] = {
  val info = Log[F].putStrLn(s"Adding 10 secs for ${task.skill.name}")

  <span class="fragment">val attemptCount: F[Int]  = AttemptState[F].get.map(_.getOrElse(task.skill, 0))</span>
  <span class="fragment">val incrementAttemptCount = AttemptState[F].modify(_ |+| Map(task.skill -> 1))</span>
  <span class="fragment">val markSkillLearned      = SkillState[F].modify(_ + task.skill)</span>

  val learnSkill =
    <span class="fragment">attemptCount.flatMap {
      <span class="fragment">case attemptsSoFar if attemptsSoFar >= 4 => markSkillLearned</span>
      <span class="fragment">case _                                   => incrementAttemptCount</span>
    }</span>

  info *> learnSkill.as(10.seconds)
}
</span>
