case class User(id: Long, name: String, age: Int)

case class UserToCreate(name: String, age: Int)

def validateUser(toCreate: UserToCreate): ValidatedNel[String, User] = {
  val checkName: ValidatedNel[String, String] =
    toCreate.name.valid
      .ensure("Name too short")(_.length >= 2)
      .ensure("Name too long")(_.length <= 30)
      .toValidatedNel

  val checkAge: ValidatedNel[String, Int] = toCreate.age.valid
    .ensure("Age too low")(_ >= 18)
    .ensure("Age too high")(_ <= 200)
    .toValidatedNel

  (
    0.valid,
    checkName,
    checkAge
  ).mapN(User)
}
