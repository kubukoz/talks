package com.kubukoz.tracing

import cats.effect.IO
import cats.data.OptionT
import java.{util => ju}
import cats.implicits._
import org.http4s.Headers
import org.http4s.util.CaseInsensitiveString
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.Header

final case class Span(traceId: String, spanId: String) {
  def toTraceMap: Map[String, String] = Map("X-B3-TraceId" -> traceId)
  def toMap: Map[String, String] = toTraceMap + ("X-B3-SpanId" -> spanId)

  def toTraceHeaders: Headers = Headers(toTraceMap.map((Header.apply _).tupled).toList)
}

object Span {
  private val newUUID = IO(ju.UUID.randomUUID().toString())
  private val logger = Slf4jLogger.getLogger[IO]

  val create: IO[Span] = (newUUID, newUUID).mapN(Span(_, _))

  def fromHeaders(headers: Headers): IO[Span] = {

    val traceId = OptionT
      .fromOption[IO](headers.get(CaseInsensitiveString("X-B3-TraceId")))
      .map(_.value)
      .getOrElseF(newUUID.flatTap(trace => logger.info("Created new trace: " + trace)))

    val spanId = newUUID.flatTap(trace => logger.info("Created new span: " + trace))

    (traceId, spanId).mapN(Span(_, _))
  }
}
