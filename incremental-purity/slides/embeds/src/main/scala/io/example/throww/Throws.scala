package io.example.throww
import java.util.UUID

object Throws {

  def registerUser(user: User): String = {
    validate(user)
    save(user).id
  }

  private def validate(user: User): Unit =
    if (user.name.isEmpty)
      throw new IllegalArgumentException("foo")

  private def save(user: User): User = {
    println("saving user")
    val newId = UUID.randomUUID()
    user.copy(id = newId.toString)
  }
}

case class User(id: String, name: String)
