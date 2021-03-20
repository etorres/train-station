package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals
import arrival.Arrivals.{Arrival, ArrivalError}
import departure.Departures
import departure.Departures.{Departure, DepartureError}
import json.infrastructure.EventJsonProtocol

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

object AllHttpRoutes extends EventJsonProtocol {
  def arrivalRoutes[F[_]: Sync](A: Arrivals[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] { case req @ POST -> Root / "arrival" =>
      for {
        arrival <- req.as[Arrival]
        result <- A.register(arrival)
        response <- result.fold(
          error =>
            error match {
              case ArrivalError.UnexpectedTrain(trainId) =>
                BadRequest(s"Unexpected train ${trainId.show}")
            },
          arrivalEvent => Created(arrivalEvent.id)
        )
      } yield response
    }
  }

  def departureRoutes[F[_]: Sync](D: Departures[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] { case req @ POST -> Root / "departure" =>
      for {
        departure <- req.as[Departure]
        result <- D.register(departure)
        response <- result.fold(
          error =>
            error match {
              case DepartureError.UnexpectedDestination(destination) =>
                BadRequest(s"Unexpected destination ${destination.show}")
            },
          departureEvent => Created(departureEvent.id)
        )
      } yield response
    }
  }
}
