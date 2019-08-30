package com.example.streams.streaming

import cats.effect.Console.io._
import cats.effect.Concurrent
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import fs2.Pipe
import fs2.Stream
import fs2.concurrent
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Decoder
import javax.jms.ConnectionFactory
import javax.jms.Message
import javax.jms.MessageConsumer
import javax.jms.TextMessage
import org.apache.activemq.ActiveMQConnectionFactory

trait Listener[F[_]] {
  def run[A: Decoder]: Stream[F, A]
}

object Listener {

  def jms(
    connFactory: ConnectionFactory
  )(
    implicit c: Concurrent[IO],
    logger: Logger[IO]
  ): Listener[IO] = new Listener[IO] {

    private val makeConsumer: Resource[IO, MessageConsumer] =
      for {
        conn <- Resource.make(IO(connFactory.createConnection()))(
                 c => IO(c.close())
               )
        _ <- Resource.liftF(IO(conn.start()))
        session <- Resource.make(
                    IO(
                      conn.createSession(
                        false,
                        javax.jms.Session.AUTO_ACKNOWLEDGE
                      )
                    )
                  )(c => IO(c.close()))
        _ <- Resource.liftF(IO(session.run()))
        consumer <- Resource.make(
                     IO(
                       session.createConsumer(session.createQueue("demo"))
                     )
                   )(c => IO(c.close()))
      } yield consumer

    private def consume(consumer: MessageConsumer): Stream[IO, Message] =
      Stream.eval(concurrent.Queue.bounded[IO, Message](100)).flatMap {
        q =>
          val setListener = IO {
            consumer.setMessageListener {
              q.enqueue1(_).unsafeRunSync()
            }
          }

          q.dequeue concurrently Stream.eval_(setListener)
      }

    private val filterText: Pipe[IO, Message, TextMessage] =
      _.evalMap {
        case msg: TextMessage => msg.some.pure[IO]
        case msg              => logger.error(s"Not a text message: $msg").as(none)
      }.unNone

    private def decode[A: Decoder]: Pipe[IO, String, A] =
      _.map(io.circe.parser.decode[A](_)).evalMap {
        case Right(v) => v.some.pure[IO]
        case Left(e)  => logger.error(e)("Decoding error").as(none)
      }.unNone

    def run[A: Decoder]: Stream[IO, A] =
      Stream
        .resource(makeConsumer)
        .flatMap(consume)
        .through(filterText)
        .map(_.getText)
        .through(decode[A])
  }
}
/*
object JMSDemo extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLoggerFromClass[IO](classOf[Listener[IO]])

  override def run(args: List[String]): IO[ExitCode] =
    Stream
      .eval(IO(new ActiveMQConnectionFactory("admin", "admin", "tcp://localhost:61616")))
      .flatMap(Listener.jms(_).run[String])
      .evalMap { msg =>
        putStrLn(msg)
      }
      .compile
      .drain
      .as(ExitCode.Success)

}
 */
