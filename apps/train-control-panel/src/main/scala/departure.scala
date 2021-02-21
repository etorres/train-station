package es.eriktorr

import event.Event.Departed
import station.Station
import station.Station.TravelDirection.Destination
import time.Moment
import time.Moment.When.{Actual, Expected}
import train.TrainId

object departure {
  final case class Departure(
    trainId: TrainId,
    to: Station[Destination],
    expected: Moment[Expected],
    actual: Moment[Actual]
  )

  sealed trait DepartureError

  object DepartureError {
    final case class UnexpectedDestination(station: Station[Destination]) extends DepartureError
  }

  trait Departures[F[_]] {
    def register(departure: Departure): F[Either[DepartureError, Departed]]
  }
}
