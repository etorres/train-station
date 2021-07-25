package es.eriktorr.train_station
package http.infrastructure

import cats.implicits._
import io.janstenpickle.trace4cats.model.{SpanId, TraceId}
import org.http4s.{Header, Headers}
import org.typelevel.ci._

final case class B3Headers(traceId: TraceId, spanId: SpanId, sampled: Int = 1)

object B3Headers {
  def toHeaders(b3Headers: Option[B3Headers]): Headers =
    b3Headers
      .fold(Headers.empty)(hs =>
        Headers(
          List(
            Header.Raw(ci"X-B3-TraceId", hs.traceId.show),
            Header.Raw(ci"X-B3-SpanId", hs.spanId.show),
            Header.Raw(ci"X-B3-Sampled", hs.sampled.show)
          )
        )
      )
}
