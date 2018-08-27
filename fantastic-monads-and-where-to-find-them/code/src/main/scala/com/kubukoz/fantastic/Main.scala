package com.kubukoz.fantastic

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.kubukoz.fantastic.dao.{BookDao, InMemoryBookDao}
import com.kubukoz.fantastic.routes.MainRoutes
import com.kubukoz.fantastic.services.BookService
import com.kubukoz.fantastic.syntax._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.io.StdIn
import scala.language.higherKinds

object Main {
  implicit private val system: ActorSystem             = ActorSystem("fantastic")
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  //mapK from mainecoon
  private val bookDao: BookDao[Result]         = new InMemoryBookDao[Task]
  private val bookService: BookService[Result] = new BookService[Result](bookDao)
  private val routes: Route                    = new MainRoutes(bookService).mainRoutes

  def main(args: Array[String]): Unit = {
    runServer()
  }

  private def runServer(): Unit = {
    val port   = 8080
    val server = Http().bindAndHandle(routes, "localhost", port)

    StdIn.readLine()

    server.flatMap(_.unbind()).onComplete { _ =>
      system.terminate()
    }
  }
}
