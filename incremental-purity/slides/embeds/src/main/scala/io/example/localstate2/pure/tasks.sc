val dishes  = Task(Skill("Dishes"))
val laundry = Task(Skill("Laundry"))

val tasks: SyncIO[List[Task]] = {
  val taskList = List.fill(10)(dishes) ::: List.fill(6)(laundry)

  SyncIO(util.Random.shuffle(taskList))
}
