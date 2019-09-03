package com.example.streams

import cats.effect._

import scala.concurrent.duration._
import cats.effect.concurrent.MVar
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import fs2.Stream
import cats.implicits._

object TCPServer extends IOApp {

  def slowDownEveryNTicks[A](
    resets: Stream[IO, Unit],
    n: Int
  ): Stream[IO, FiniteDuration] = {
    val showSlowingDown =
      Stream.eval_(IO(println("---------- Slowing down! ----------")))

    val showResetting =
      IO(println("---------- Resetting delays! ----------"))

    val delaysExponential: Stream[IO, FiniteDuration] =
      Stream.iterate(1.millisecond)(_ * 2).flatMap {
        Stream.awakeDelay[IO](_).take(n.toLong) ++ showSlowingDown
      }

    Stream.eval(MVar.empty[IO, Unit]).flatMap { restart =>
      val delaysUntilReset =
        delaysExponential.interruptWhen(restart.take.attempt)

      //restart stream emitting sleeps every time an element is produced by resets
      delaysUntilReset.repeat concurrently
        resets.evalTap(_ => restart.put(()) *> showResetting)
    }
  }

  val slowDown =
    slowDownEveryNTicks(
      resets = Stream.awakeEvery[IO](3.seconds).void,
      n = 10
    )

  val acgResource = Resource
    .make(IO(Executors.newCachedThreadPool()))(e => IO(e.shutdown()))
    .flatMap { es =>
      Resource.make(IO(AsynchronousChannelGroup.withThreadPool(es)))(
        acg => IO(acg.shutdown())
      )
    }

  val server: Stream[IO, Resource[IO, fs2.io.tcp.Socket[IO]]] =
    Stream.resource(acgResource).flatMap { implicit acg =>
      fs2
        .io
        .tcp
        .Socket
        .server[IO](
          new InetSocketAddress("0.0.0.0", 8080)
        )
    }

  val logSocket = Resource.make(IO(println("new socket")))(
    _ => IO(println("closed socket"))
  )

  val clientMessages = server
    .map(logSocket *> _)
    .map {
      Stream
        .resource(_)
        .flatMap(_.reads(1024))
        .through(fs2.text.utf8Decode)
        .through(fs2.text.lines)
        .map("Message: " + _)
      // .unchunk uncomment me for more interleaving
    }
    .parJoin(maxOpen = 10)

  def run(args: List[String]): IO[ExitCode] =
    IO(println("Started app")) *>
      clientMessages
        .zipLeft(slowDown)
        .showLinesStdOut
        .compile
        .drain
        .as(ExitCode.Success)
}
