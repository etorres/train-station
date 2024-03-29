package es.eriktorr.train_station
package http.infrastructure

import cats.data.{Kleisli, OptionT}
import cats.implicits._
import cats.{Applicative, Defer}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.base.context.Provide
import io.janstenpickle.trace4cats.inject.Trace
import io.janstenpickle.trace4cats.model.SampleDecision
import org.http4s._
import org.typelevel.ci._
import cats.effect.MonadCancelThrow

object B3Propagation {
  def make[F[_]: Applicative: Trace, G[_]: Defer: MonadCancelThrow](service: HttpRoutes[G])(implicit
    P: Provide[F, G, Span[F]]
  ): HttpRoutes[G] = Kleisli { request: Request[G] =>
    for {
      response <- service(request)
      b3Headers <-
        OptionT.liftF(
          P
            .ask[Span[F]]
            .map { span =>
              List(
                Header.Raw(ci"X-B3-TraceId", span.context.traceId.show),
                Header.Raw(ci"X-B3-SpanId", span.context.spanId.show),
                Header.Raw(
                  ci"X-B3-Sampled",
                  span.context.traceFlags.sampled match {
                    case SampleDecision.Drop => "0"
                    case SampleDecision.Include => "1"
                  }
                )
              ) ++ span.context.parent
                .map(parent => Header.Raw(ci"X-B3-ParentSpanId", parent.spanId.show))
                .fold(List.empty[Header.Raw])(List(_))
            }
        )
    } yield response.putHeaders(b3Headers.map(Header.ToRaw.rawToRaw): _*)
  }
}
