package com.example.streams.nonstreaming

import io.circe.Decoder
import io.circe.parser._
import javax.jms._

class JMSConsumer[A: Decoder](consumer: MessageConsumer) {

  def start(handler: A => Unit): Unit =
    consumer.setMessageListener {
      case msg: TextMessage =>
        decode[A](msg.getText()) match {
          case Left(error)  => throw error
          case Right(value) => handler(value)
        }
    }

  def shutdown(): Unit = consumer.close()
}

final case class Event(id: String)

class Demo(consumer: JMSConsumer[Event]) {

  def run(): Unit =
    consumer.start { event =>
      println(event.id)
    }
}
