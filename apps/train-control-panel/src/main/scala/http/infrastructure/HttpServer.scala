package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals

import cats.effect.{ConcurrentEffect, Timer}
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{Logger => Http4sLogger}
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext

object HttpServer {
  def stream[F[_]: ConcurrentEffect: Timer](
    executionContext: ExecutionContext
  )(implicit L: Logger[F]): Stream[F, Nothing] = {
    val arrivals = Arrivals.impl[F](???, ???, ???)

    val httpApp = AllHttpRoutes.arrivalRoutes[F](arrivals).orNotFound
    val finalHttpApp = Http4sLogger.httpApp(logHeaders = true, logBody = true)(httpApp)

    BlazeServerBuilder[F](executionContext)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(finalHttpApp)
      .serve
  }.drain
}
