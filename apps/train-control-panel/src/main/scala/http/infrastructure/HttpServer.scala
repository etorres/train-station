package es.eriktorr.train_station
package http.infrastructure

import TrainControlPanelConfig.HttpServerConfig
import arrival.Arrivals
import departure.Departures

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits._
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext

object HttpServer {
  def stream[F[_]: ConcurrentEffect: Timer](
    arrivals: Arrivals[F],
    departures: Departures[F],
    executionContext: ExecutionContext,
    httpServerConfig: HttpServerConfig
  ): Stream[F, Nothing] = {
    val httpApp = (AllHttpRoutes.arrivalRoutes[F](arrivals) <+> AllHttpRoutes.departureRoutes[F](
      departures
    )).orNotFound
    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    BlazeServerBuilder[F](executionContext)
      .bindHttp(httpServerConfig.port.value, httpServerConfig.host.value)
      .withHttpApp(finalHttpApp)
      .serve
  }.drain
}
