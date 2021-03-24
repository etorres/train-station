package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals
import arrival.Arrivals.{Arrival, ArrivalError}
import departure.Departures
import departure.Departures.Departure
import departure.Departures.DepartureError.UnexpectedDestination
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
        response <- A
          .register(arrival)
          .flatMap(arrivalEvent => Created(arrivalEvent.id))
          .recoverWith { case ArrivalError.UnexpectedTrain(trainId) =>
            BadRequest(show"Unexpected train $trainId")
          }
      } yield response
    }
  }

  def departureRoutes[F[_]: Sync](D: Departures[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] { case req @ POST -> Root / "departure" =>
      for {
        departure <- req.as[Departure]
        response <- D
          .register(departure)
          .flatMap(departureEvent => Created(departureEvent.id))
          .recoverWith { case UnexpectedDestination(destination) =>
            BadRequest(show"Unexpected destination $destination")
          }
      } yield response
    }
  }
}
