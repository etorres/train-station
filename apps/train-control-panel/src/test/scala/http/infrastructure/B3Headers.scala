package es.eriktorr.train_station
package http.infrastructure

import cats.implicits._
import io.janstenpickle.trace4cats.model.{SpanId, TraceId}
import org.http4s.Header

final case class B3Headers(traceId: TraceId, spanId: SpanId, sampled: Int = 1)

object B3Headers {
  def toHeaders(b3Headers: Option[B3Headers]): List[Header] =
    b3Headers.fold(List.empty[Header])(hs =>
      List(
        Header("X-B3-TraceId", hs.traceId.show),
        Header("X-B3-SpanId", hs.spanId.show),
        Header("X-B3-Sampled", hs.sampled.show)
      )
    )
}
