package com.kubukoz.tracing

import cats.effect.IO
import cats.data.OptionT
import cats.implicits._
import org.http4s.Headers
import org.http4s.util.CaseInsensitiveString
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.Header
import kamon.trace.Identifier

final case class Span(
  name: String,
  traceId: String,
  spanId: String,
  parentSpanId: Option[String]
) {
  def withName(newName: String) = copy(name = newName)

  def toMap: Map[String, String] =
    Map("X-B3-TraceId" -> traceId, "X-B3-SpanId" -> spanId) ++ Map(
      "X-B3-ParentSpanId" -> parentSpanId
    ).flattenOption

  def toTraceHeaders: Headers = Headers(toMap.map((Header.apply _).tupled).toList)
}

object Span {

  private val newUUID =
    IO(Identifier.Factory.EightBytesIdentifier.generate()).map(_.string)

  private val logger = Slf4jLogger.getLogger[IO]

  def create(name: String, parent: Option[Span] = None): IO[Span] =
    parent match {
      case Some(parent) => newUUID.map(Span(name, parent.traceId, _, parent.spanId.some))
      case None         => (newUUID, newUUID).mapN(Span(name, _, _, none))
    }

  def fromHeaders(name: String)(headers: Headers): IO[Span] = {

    def readF(name: String): OptionT[IO, String] =
      OptionT.fromOption[IO](headers.get(CaseInsensitiveString(name))).map(_.value)

    val traceId = readF("X-B3-TraceId")
    val spanId = readF("X-B3-SpanId")
    val parentId = readF("X-B3-ParentSpanId")

    traceId.value.flatMap {
      case Some(traceId) =>
        (spanId.getOrElseF(newUUID), parentId.value).tupled.map {
          case (spanId, parentSpanId) =>
            Span(
              name = name,
              traceId = traceId,
              spanId = spanId,
              parentSpanId = parentSpanId
            )
        }

      case None =>
        (newUUID, newUUID)
          .tupled
          .map {
            case (traceId, spanId) =>
              Span(
                name = name,
                traceId = traceId,
                spanId = spanId,
                parentSpanId = None
              )
          }
          .flatTap(span => logger.info(span.toMap)(s"Created new trace and span"))
    }
  }
}
