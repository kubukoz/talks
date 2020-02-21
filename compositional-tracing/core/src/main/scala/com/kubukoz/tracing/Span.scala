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
  def toMap: Map[String, String] = Map("X-B3-TraceId" -> traceId, "X-B3-SpanId" -> spanId)

  def toTraceHeaders: Headers = Headers(toMap.map((Header.apply _).tupled).toList)
}

object Span {
  private val newUUID =
    IO(Identifier.Factory.EightBytesIdentifier.generate()).map(_.string)

  private val logger = Slf4jLogger.getLogger[IO]

  def create(name: String): IO[Span] = (newUUID, newUUID).mapN(Span(name, _, _, none))

  def fromHeaders(headers: Headers): IO[Span] = {

    val traceId = OptionT
      .fromOption[IO](headers.get(CaseInsensitiveString("X-B3-TraceId")))
      .map(_.value)
      .getOrElseF(newUUID.flatTap(trace => logger.info("Created new trace: " + trace)))

    val parentId = headers.get(CaseInsensitiveString("X-B3-SpanId")).map(_.value)

    (traceId, newUUID).tupled.flatMap {
      case (trace, span) =>
        val newSpan = Span("no-name", trace, span, parentId)

        logger.info(newSpan.toMap)(s"Created new span: $span").as(newSpan)
    }
  }
}
