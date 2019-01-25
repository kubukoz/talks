package io.example

import cats.effect.{IO, Resource}

class Lock

object ResourceSimple {

  def lock(name: String): Resource[IO, Lock] = {
    val acquire             = IO(println(s"Acquiring $name")).map(_ => new Lock)
    def cleanup(lock: Lock) = IO(println(s"Releasing $name (lock: $lock)"))

    Resource.make(acquire)(cleanup)
  }
}
