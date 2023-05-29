package es.eriktorr.train_station
package http.infrastructure

import TrainControlPanelConfig.HttpServerConfig
import arrival.Arrivals
import arrival.syntax._
import departure.Departures
import departure.syntax._

import cats.data.Kleisli
import cats.effect.{Async, Temporal}
import cats.implicits._
import fs2.compression.Compression
import fs2.io.net.Network
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.http4s.common.Http4sRequestFilter
import io.janstenpickle.trace4cats.http4s.server.syntax._
import io.janstenpickle.trace4cats.inject.{EntryPoint, Trace}
import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.{CORS, GZip, Logger => Http4sLogger}

object HttpServer {
  def stream[F[_]: Async: Compression: Network: Temporal: Trace](
    arrivals: Arrivals[F],
    departures: Departures[F],
    entryPoint: EntryPoint[F],
    httpServerConfig: HttpServerConfig
  ): F[Unit] = {

    val finalHttpApp = Http4sLogger.httpApp(logHeaders = true, logBody = true)(
      httpApp(arrivals, departures, entryPoint)
    )

    EmberServerBuilder
      .default[F]
      .withHost(httpServerConfig.host)
      .withPort(httpServerConfig.port)
      .withHttpApp(CORS.policy.withAllowOriginAll(GZip(finalHttpApp)))
      .build
      .use(_ => F.never)
  }

  def httpApp[F[_]: Async: Temporal: Trace](
    arrivals: Arrivals[F],
    departures: Departures[F],
    entryPoint: EntryPoint[F]
  ): HttpApp[F] =
    (logicRoutes(arrivals, departures, entryPoint) <+> docsRoute(entryPoint)).orNotFound

  def logicRoutes[F[_]: Async: Trace](
    arrivals: Arrivals[F],
    departures: Departures[F],
    entryPoint: EntryPoint[F]
  ): HttpRoutes[F] = B3Propagation
    .make[F, Kleisli[F, Span[F], *]](
      OpenApiEndpoints.routes(
        arrivals.liftTrace[Kleisli[F, Span[F], *]],
        departures.liftTrace[Kleisli[F, Span[F], *]]
      )
    )
    .inject(entryPoint, requestFilter = Http4sRequestFilter.kubernetesPrometheus)

  def docsRoute[F[_]: Async: Trace](entryPoint: EntryPoint[F]): HttpRoutes[F] =
    OpenApiEndpoints.swaggerRoute[Kleisli[F, Span[F], *]].inject(entryPoint)
}
