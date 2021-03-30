package es.eriktorr.train_station
package http.infrastructure

import TrainControlPanelConfig.HttpServerConfig
import arrival.Arrivals
import departure.Departures
import trace.infrastructure.TraceContext

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits._
import fs2.Stream
import io.janstenpickle.trace4cats.http4s.common.Http4sRequestFilter
import io.janstenpickle.trace4cats.http4s.server.Http4sResourceKleislis
import io.janstenpickle.trace4cats.http4s.server.syntax._
import io.janstenpickle.trace4cats.inject.{EntryPoint, Trace}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, GZip, Logger => Http4sLogger}

import scala.concurrent.ExecutionContext

object HttpServer {
  def stream[F[_]: ConcurrentEffect: Timer: Trace](
    arrivals: Arrivals[F],
    departures: Departures[F],
    httpServerConfig: HttpServerConfig,
    executionContext: ExecutionContext,
    entryPoint: EntryPoint[F]
  ): Stream[F, Nothing] = {

//    val httpApp = (AllHttpRoutes.arrivalRoutes[Kleisli[F, Span[F], *]](
//      ArrivalsTracer.liftTrace[F, Kleisli[F, Span[F], *]](arrivals)
//    ) <+> AllHttpRoutes.departureRoutes[Kleisli[F, Span[F], *]](
//      DeparturesTracer.liftTrace[F, Kleisli[F, Span[F], *]](departures)
//    )).inject(
//      entryPoint,
//      // makeContext = TraceContext.make[F],
//      requestFilter = Http4sRequestFilter.kubernetesPrometheus
//    ).orNotFound

//    val httpApp = (AllHttpRoutes.arrivalRoutes[Kleisli[F, TraceContext[F], *]](
//      ArrivalsTracer.liftTraceContext[F, Kleisli[F, TraceContext[F], *], TraceContext[F]](
//        arrivals,
//        TraceContext.span[F]
//      )
//    ) <+> AllHttpRoutes.departureRoutes[Kleisli[F, TraceContext[F], *]](
//      DeparturesTracer.liftTraceContext[F, Kleisli[F, TraceContext[F], *], TraceContext[F]](
//        departures,
//        TraceContext.span[F]
//      )
//    )).injectContext(
//      entryPoint,
//      makeContext = TraceContext.make[F],
//      requestFilter = Http4sRequestFilter.kubernetesPrometheus
//    ).orNotFound

//    implicit val traceContextTrace: Trace[Kleisli[F, TraceContext[F], *]] =
//      Trace.kleisliInstance[F].lens[TraceContext[F]](_.span, (c, span) => c.copy(span = span))

//    val httpApp = (AllHttpRoutes.arrivalRoutes[Kleisli[F, TraceContext[F], *]](
//      ArrivalsTracer.liftTraceContext[F, Kleisli[F, TraceContext[F], *], TraceContext[F]](
//        arrivals,
//        TraceContext.span[F]
//      )
//    ) <+> AllHttpRoutes.departureRoutes[Kleisli[F, TraceContext[F], *]](
//      DeparturesTracer.liftTraceContext[F, Kleisli[F, TraceContext[F], *], TraceContext[F]](
//        departures,
//        TraceContext.span[F]
//      )
//    )).tracedContext(
//      Http4sResourceKleislis.fromHeadersContext(
//        TraceContext.make[F],
//        requestFilter = Http4sRequestFilter.kubernetesPrometheus
//      )(entryPoint.toKleisli)
//    ).orNotFound

    implicit val traceContextTrace: Trace[Kleisli[F, TraceContext[F], *]] =
      TraceContext.traceContextTrace

    val httpApp =
      (AllTracedRoutes.traced(arrivals) <+> AllTracedRoutes.traced(departures))
        .tracedContext(
          Http4sResourceKleislis.fromHeadersContext(
            TraceContext.make[F],
            requestFilter = Http4sRequestFilter.kubernetesPrometheus
          )(entryPoint.toKleisli)
        )
        .orNotFound

    val finalHttpApp = Http4sLogger.httpApp(logHeaders = true, logBody = true)(httpApp)

    BlazeServerBuilder[F](executionContext)
      .bindHttp(httpServerConfig.port.value, httpServerConfig.host.value)
      .withHttpApp(CORS(GZip(finalHttpApp)))
      .serve
  }.drain
}
