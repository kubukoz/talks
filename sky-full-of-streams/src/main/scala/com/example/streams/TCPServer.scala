package com.example.streams

import cats.effect._

import scala.concurrent.duration._
import cats.effect.concurrent.MVar
import java.net.InetSocketAddress

import fs2.Stream
import cats.implicits._
import fs2.Pipe

object TCPServer extends IOApp {

  val address = new InetSocketAddress("0.0.0.0", 8080)

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

  def server(
    blocker: Blocker
  ): Stream[IO, Resource[IO, fs2.io.tcp.Socket[IO]]] =
    Stream.resource(fs2.io.tcp.SocketGroup[IO](blocker)).flatMap {
      group =>
        group.server[IO](
          address
        )
    }

  val logSocket = Resource.make(IO(println("new socket")))(
    _ => IO(println("closed socket"))
  )

  def showChunkSize[A]: fs2.Pipe[IO, A, A] =
    _.chunks
      .evalTap { chunk =>
        IO(println("chunk size: " + chunk.size))
      }
      .flatMap(Stream.chunk)

  val clientMessages = Stream
    .resource(Blocker[IO])
    .flatMap(server)
    .map(logSocket *> _)
    .map {
      Stream
        .resource(_)
        .flatMap(_.reads(1024))
        .through(fs2.text.utf8Decode)
        .through(fs2.text.lines)
        .map("Message: " + _)
      // .through(showChunkSize)
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

object TCPClient extends IOApp {

  def clientPipe(blocker: Blocker): Pipe[IO, Byte, Unit] = {
    val clientResource = fs2
      .io
      .tcp
      .SocketGroup[IO](blocker)
      .flatMap(_.client[IO](TCPServer.address))

    stream =>
      Stream
        .resource(clientResource)
        .map(_.writes())
        .flatMap(stream.through(_))
  }

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { blocker =>
      fs2
        .io
        .stdin[IO](4096, blocker)
        .through(fs2.text.utf8Decode[IO])
        .through(fs2.text.lines[IO])
        .map(_ + "\n")
        .flatMap(s => Stream.emits(s.getBytes()))
        .through(clientPipe(blocker))
        .compile
        .drain
    } as ExitCode.Success
}
