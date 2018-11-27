package io.example.actors

import java.util.concurrent.{Executors, ForkJoinPool}

import cats.effect.{ExitCode, IO, IOApp}
import fs2.concurrent.Queue
import cats.implicits._
import cats.effect.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

sealed trait Message; object Message extends Message

object QueueMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val ecIO = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
    val readLine: IO[Unit] = IO(ecIO).bracket { ec =>
      fs2.io.stdin[IO](4192, ec).head.compile.drain
    }(ec => IO(ec.shutdown()))

    val prog = for {
      q <- Queue.bounded[IO, Message](100)

      producer = fs2.Stream.fixedRate(1.second).as(Message).to(q.enqueue).compile.drain
      consumer = q.dequeue.evalTap[IO](msg => IO(println(msg))).compile.drain

      _ <- (producer, consumer).parTupled
    } yield ()

    prog.race(readLine).as(ExitCode.Success)
  }
}
