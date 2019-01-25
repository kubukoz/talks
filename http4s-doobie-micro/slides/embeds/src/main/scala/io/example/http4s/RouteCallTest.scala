package io.example.http4s

import cats.effect.IO
import cats.implicits._
import org.http4s.Method._
import org.http4s.{HttpApp, Request, Uri}

//stubs
object Imports {
  implicit class AnyShouldBe(a: Any) {
    def shouldBe(another: Any): Boolean = true
  }
}

import Imports._

object RouteCallTest {
  val route: HttpApp[IO] = HttpApp.notFound[IO]

  val result = route(Request(method = GET, uri = Uri.uri("/home")))

  result.flatMap(_.bodyAsText.compile.foldMonoid).map {
    _ shouldBe "Not found"
  }
}
