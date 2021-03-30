package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals
import arrival.infrastructure.ArrivalsTracer
import departure.Departures
import departure.infrastructure.DeparturesTracer
import trace.infrastructure.TraceContext

import cats.Defer
import cats.data.Kleisli
import cats.effect.{BracketThrow, ConcurrentEffect}
import io.janstenpickle.trace4cats.inject.Trace
import org.http4s.HttpRoutes

object AllTracedRoutes {
  def traced[F[_]: ConcurrentEffect: Defer: BracketThrow](
    arrivals: Arrivals[F]
  )(implicit
    traceContextTrace: Trace[Kleisli[F, TraceContext[F], *]]
  ): HttpRoutes[Kleisli[F, TraceContext[F], *]] =
    AllHttpRoutes.routes[Kleisli[F, TraceContext[F], *]](
      ArrivalsTracer.liftTraceContext[F, Kleisli[F, TraceContext[F], *], TraceContext[F]](
        arrivals,
        TraceContext.span[F]
      )
    )

  def traced[F[_]: ConcurrentEffect: Defer: BracketThrow](
    departures: Departures[F]
  )(implicit
    traceContextTrace: Trace[Kleisli[F, TraceContext[F], *]]
  ): HttpRoutes[Kleisli[F, TraceContext[F], *]] =
    AllHttpRoutes.routes[Kleisli[F, TraceContext[F], *]](
      DeparturesTracer.liftTraceContext[F, Kleisli[F, TraceContext[F], *], TraceContext[F]](
        departures,
        TraceContext.span[F]
      )
    )
}
