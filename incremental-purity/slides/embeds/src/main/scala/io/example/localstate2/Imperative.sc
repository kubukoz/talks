package io.example.localstate2

object Imperative {
  import scala.concurrent.duration._

  def solveTasks(tasks: List[Task]): FiniteDuration = {
    <span class="fragment">var timeSoFar                 = 0.seconds
    var skillsKnown: Set[Skill]   = Set.empty
    var attempts: Map[Skill, Int] = Map.empty</span>

    tasks.foreach { task =>
      <span class="fragment">if (skillsKnown.contains(task.skill)) {
        <span class="fragment">println(s"I know how to do ${task.skill.name}! Adding only 1 second")
        timeSoFar += 1.second</span>
      } else {
        <span class="fragment">attempts.get(task.skill) match {
          <span class="fragment">case Some(attemptsSoFar) if attemptsSoFar >= 4 => skillsKnown += task.skill</span>
          <span class="fragment">case Some(attemptsSoFar)                       => attempts += (task.skill -> (attemptsSoFar + 1))</span>
          <span class="fragment">case None                                      => attempts += (task.skill -> 1)</span>
        }

        println(s"Adding 10 secs for ${task.skill.name}")
        timeSoFar += 10.seconds</span>
      }</span>
    }

    timeSoFar
  }

  val dishes  = Task(Skill("Dishes"))
  val laundry = Task(Skill("Laundry"))

  val tasks: List[Task] = util.Random.shuffle(List.fill(10)(dishes) ::: List.fill(6)(laundry))

  def main(args: Array[String]): Unit = {
    println(solveTasks(tasks))
  }
}

case class Skill(name: String)
case class Task(skill: Skill)
