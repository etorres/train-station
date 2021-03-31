package es.eriktorr.train_station
package http.infrastructure

import cats.data.Kleisli
import cats.effect.BracketThrow
import cats.implicits._
import cats.{Defer, Monad}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.base.context.Provide
import io.janstenpickle.trace4cats.model.SampleDecision
import org.http4s._

object B3Middleware {
  def make[F[_]: Monad, G[_]: Defer: BracketThrow](
    service: HttpRoutes[G]
  )(implicit P: Provide[F, G, Span[F]]): HttpRoutes[G] =
    Kleisli { request: Request[G] =>
      service(request).map { response =>
        val headers = for {
          context <- P.ask[Span[F]].map(_.context)
          traceIdHeader = Header("X-B3-TraceId", context.traceId.show)
          parentSpanIdHeader = context.parent.map(parent =>
            Header("X-B3-ParentSpanId", parent.spanId.show)
          )
          spanIdHeader = Header("X-B3-SpanId", context.spanId.show)
          sampledHeader = Header(
            "X-B3-Sampled",
            context.traceFlags.sampled match {
              case SampleDecision.Drop => "0"
              case SampleDecision.Include => "1"
            }
          )
        } yield parentSpanIdHeader
          .fold(List.empty)(List(_)) ++ List(traceIdHeader, spanIdHeader, sampledHeader)
        // TODO
        val xxx = headers.map(response.putHeaders(_: _*))
        // TODO
        val yyy: G[Response[G]] = headers.map(response.putHeaders(_: _*))
        val kk: Response[G] = response.putHeaders(???)
        kk
      }
    }
}
