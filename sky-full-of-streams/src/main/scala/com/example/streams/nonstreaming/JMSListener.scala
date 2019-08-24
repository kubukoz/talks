package com.example.streams.nonstreaming

import io.circe.Decoder
import io.circe.parser._
import javax.jms._
import org.apache.activemq.ActiveMQConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait Listener {
  def create[A: Decoder](handler: A => Unit): Unit
}

class JMSListener(connectionFactory: ConnectionFactory, logger: Logger) extends Listener {

  def create[A: Decoder](handler: A => Unit): Unit =
    withConsumer { consumer =>
      consumer.setMessageListener {
        case msg: TextMessage =>
          decode[A](msg.getText()) match {
            case Left(error)  => logger.error(s"Decoding error: $error")
            case Right(value) => handler(value)
          }
        case msg => logger.error(s"Not a text message: $msg")
      }
      while (true) {}
    }

  private def withConsumer[X](f: MessageConsumer => X): X = {
    val conn = connectionFactory.createConnection()
    try {
      conn.start()
      val ses = conn.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE)

      try {
        ses.run()
        val consumer = ses.createConsumer(ses.createQueue("demo"))
        try {
          f(consumer)
        } finally {
          consumer.close()
        }
      } finally {
        ses.close()
      }
    } finally {
      conn.close()
    }

  }
}

object JMSDemo {

  def main(args: Array[String]): Unit = {
    val cf: ConnectionFactory = new ActiveMQConnectionFactory("admin", "admin", "tcp://localhost:61616")

    val listener: Listener = new JMSListener(cf, LoggerFactory.getLogger(classOf[JMSListener]))

    listener.create[String] { msg =>
      println(msg)
    }
  }
}
