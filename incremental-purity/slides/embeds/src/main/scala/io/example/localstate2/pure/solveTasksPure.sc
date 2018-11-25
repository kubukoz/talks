case class MyState(skillsKnown: Set[Skill], attempts: Map[Skill, Int])

//Chain is an append-optimized list
def solveTasksPure(tasks: List[Task]): (Chain[String], FiniteDuration) = {
  <span class="fragment">//Can write to a Chain[String], using a state of type MyState
  type S[A] = WriterT[State[MyState, ?], Chain[String], A]</span>

  <span class="fragment">implicit val console: Log[S] = s => WriterT.tell(Chain.one(s))</span>

  <span class="fragment">val startState = MyState(Set.empty, Map.empty)</span>
  <span class="fragment">
  //run `work` for each task, combine the results with Monoid[FiniteDuration]
  val result: S[FiniteDuration] = tasks.foldMapM(work[S])</span>
<span class="fragment">
  result.run.runA(startState).value</span>
}

def work[F[_]: Log: FlatMap](task: Task): F[FiniteDuration] = ???
