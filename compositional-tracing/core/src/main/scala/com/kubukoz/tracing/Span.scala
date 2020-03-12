package com.kubukoz.tracing

import cats.data.OptionT
import cats.implicits._
import org.http4s.Headers
import org.http4s.util.CaseInsensitiveString
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.Header
import kamon.trace.Identifier
import cats.effect.Sync

final case class Span(
  name: String,
  traceId: String,
  spanId: String,
  parentSpanId: Option[String],
  values: Map[String, String]
) {
  def withName(newName: String) = copy(name = newName)

  def withValues(modValues: Map[String, String] => Map[String, String]): Span =
    copy(values = modValues(values))

  def toMap: Map[String, String] =
    Map("X-B3-TraceId" -> traceId, "X-B3-SpanId" -> spanId) ++ Map(
      "X-B3-ParentSpanId" -> parentSpanId
    ).flattenOption ++ values.map {
      case (key, v) => key -> v
    }

  def toTraceHeaders: Headers = Headers(toMap.map((Header.apply _).tupled).toList)
}

object Span {

  private def newId[F[_]: Sync]: F[String] = {
    val idLength = 16L

    fs2
      .Stream
      .repeatEval(Sync[F].delay(scala.util.Random.nextInt(16)))
      .take(idLength)
      .map(_.toHexString)
      .compile
      .string
  }

  def create[F[_]: Sync](name: String, parent: Option[Span] = None): F[Span] =
    parent match {
      case Some(parent) =>
        newId[F].map(Span(name, parent.traceId, _, parent.spanId.some, Map.empty))

      case None => (newId[F], newId[F]).mapN(Span(name, _, _, none, Map.empty))
    }

  def fromHeaders[F[_]: Sync](name: String)(headers: Headers): F[Span] = {
    val logger = Slf4jLogger.getLogger[F]

    def readF(name: String): OptionT[F, String] =
      OptionT.fromOption[F](headers.get(CaseInsensitiveString(name))).map(_.value)

    val traceId = readF("X-B3-TraceId")
    val spanId = readF("X-B3-SpanId")
    val parentId = readF("X-B3-ParentSpanId")

    traceId.value.flatMap {
      case Some(traceId) =>
        (spanId.getOrElseF(newId), parentId.value).tupled.map {
          case (spanId, parentSpanId) =>
            Span(
              name = name,
              traceId = traceId,
              spanId = spanId,
              parentSpanId = parentSpanId,
              Map.empty
            )
        }

      case None =>
        (newId, newId)
          .tupled
          .map {
            case (traceId, spanId) =>
              Span(
                name = name,
                traceId = traceId,
                spanId = spanId,
                parentSpanId = None,
                Map.empty
              )
          }
          .flatTap(span => logger.info(span.toMap)(s"Created new trace and span"))
    }
  }
}
