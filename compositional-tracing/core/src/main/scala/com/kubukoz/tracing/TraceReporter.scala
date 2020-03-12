package com.kubukoz.tracing

import cats.effect.Sync
import java.time.Instant
import cats.effect.Resource
import zipkin2.Endpoint
import zipkin2.reporter.okhttp3.OkHttpSender
import zipkin2.reporter.AsyncReporter
import cats.implicits._
import cats.effect.Blocker
import cats.effect.ContextShift

trait TraceReporter[F[_]] {
  def trace[A](span: Span)(ioa: F[A]): F[A]
  def flush: F[Unit]
}

object TraceReporter {
  import scala.concurrent.duration._

  def convertSpan(endpoint: Endpoint, span: Span, start: Instant, end: Instant) = {
    val builder = zipkin2
      .Span
      .newBuilder()
      .name(span.name)
      .id(span.spanId)
      .traceId(span.traceId)
      .parentId(span.parentSpanId.orNull)
      .timestamp(start.toEpochMilli.millis.toMicros)
      .duration(java.time.Duration.between(start, end).toMillis.millis.toMicros)
      .localEndpoint(endpoint)

    span.values.foreach {
      case (k, v) =>
        builder.putTag(k, v)
    }

    builder.build()
  }

  def zipkin[F[_]: Sync: ContextShift](
    endpoint: Endpoint,
    blocker: Blocker
  ): Resource[F, TraceReporter[F]] =
    Resource
      .fromAutoCloseable(
        Sync[F].delay(
          AsyncReporter.create(OkHttpSender.create("http://localhost:9411/api/v2/spans"))
        )
      )
      .map { reporter =>
        new TraceReporter[F] {

          def trace[A](span: Span)(ioa: F[A]): F[A] = {

            val now = Sync[F].delay(Instant.now())
            (now, ioa, now)
              .mapN((before, result, after) =>
                (result, convertSpan(endpoint, span, before, after))
              )
              .flatMap {
                case (result, zipSpan) =>
                  Sync[F].delay(reporter.report(zipSpan)).as(result)
              }
          }

          val flush: F[Unit] = blocker.delay(reporter.flush())
        }
      }
}
