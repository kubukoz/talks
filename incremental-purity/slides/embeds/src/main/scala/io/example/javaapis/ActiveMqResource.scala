package io.example.javaapis
import java.util.UUID
import java.util.concurrent.Executors

import cats.effect.implicits._
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import fs2.concurrent.Queue
import javax.jms._
import org.apache.activemq.ActiveMQConnectionFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ActiveMqResource extends IOApp {
  import ActiveMqUtils._

  val blockingEcF: Resource[IO, ExecutionContext] = {
    Resource
      .make(IO(Executors.newCachedThreadPool()).map(ExecutionContext.fromExecutorService))(ec => IO(ec.shutdown()))
      .widen[ExecutionContext]
  }

  override def run(args: List[String]): IO[ExitCode] = blockingEcF.use { blockingEc =>
    val readLine: IO[Unit] = fs2.io.stdin[IO](8192, blockingEc).head.compile.drain

    val runPrograms: IO[Unit] = (
      senderProgram(workers = 10),
      receiverProgram(workers = 10)(blockingEc)
    ).parTupled.void

    runPrograms
      .race(readLine)
      .as(ExitCode.Success)
  }

  def senderProgram(workers: Int): IO[Nothing] = producer("MyQueue").use {
    case (ses, prod) =>
      val msg = IO(UUID.randomUUID().toString)

      val doSend = msg.flatMap { text =>
        IO(println(s"Sending $text")) *>
          IO(prod.send(ses.createTextMessage(text))) *>
          IO.sleep(1.second)
      }

      List.fill(workers)(doSend).parSequence.foreverM
  }

  def receiverProgram(workers: Int)(blockingEc: ExecutionContext): IO[Unit] = consumer("MyQueue").use { cons =>
    streamMessages(cons).collect { case m: TextMessage => m.getText }
      .mapAsync(workers)(msg => contextShift.evalOn(blockingEc)(IO(println(s"Received: $msg"))))
      .compile
      .drain
  }

  private def streamMessages(cons: MessageConsumer): fs2.Stream[IO, Message] =
    for {
      q <- fs2.Stream.eval(Queue.bounded[IO, Message](maxSize = 1000))
      _ <- fs2.Stream
        .eval(IO(cons.setMessageListener(msg => q.enqueue1(msg).unsafeRunSync())))
        .onFinalize(IO(cons.setMessageListener(null)) *> IO(println("Listener has been unset")))
      msg <- q.dequeue
    } yield msg
}

object ActiveMqUtils {

  val factoryIO: IO[ActiveMQConnectionFactory] = IO {
    new ActiveMQConnectionFactory(
      "admin",
      "admin",
      "tcp://localhost:61616"
    )
  }

  val sessionF: Resource[IO, Session] =
    for {
      factory <- Resource.liftF(factoryIO)
      conn    <- Resource.make(IO(factory.createConnection()))(conn => IO(conn.close()))
      _       <- Resource.make(IO(conn.start()))(_ => IO(conn.close()))
      ses     <- Resource.make(IO(conn.createSession(false, Session.AUTO_ACKNOWLEDGE)))(ses => IO(ses.close()))
    } yield ses

  def producer(queueName: String): Resource[IO, (Session, MessageProducer)] = {
    for {
      ses  <- sessionF
      dest <- Resource.liftF(IO(ses.createQueue(queueName)))
      prod <- Resource.make(IO(ses.createProducer(dest)))(prod => IO(prod.close()))
    } yield (ses, prod)
  }

  def consumer(queueName: String): Resource[IO, MessageConsumer] = {
    for {
      ses  <- sessionF
      dest <- Resource.liftF(IO(ses.createQueue(queueName)))
      cons <- Resource.make(IO(ses.createConsumer(dest)))(cons => IO(cons.close()))
    } yield cons
  }

}
