package es.eriktorr.train_station
package departure

import departure.Departures.Departure
import departure.Departures.DepartureError.UnexpectedDestination
import event.Event.Departed
import event.EventId
import json.infrastructure.{MomentJsonProtocol, StationJsonProtocol, TrainJsonProtocol}
import messaging.EventSender
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment
import time.Moment.When.{Actual, Created, Expected}
import train.TrainId
import uuid.UUIDGenerator

import cats.data.NonEmptyList
import cats.derived.semiauto
import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Show}
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.typelevel.log4cats.Logger

import scala.util.control.NoStackTrace

trait Departures[F[_]] {
  def register(departure: Departure): F[Departed]
}

object Departures {
  final case class Departure(
    trainId: TrainId,
    to: Station[Destination],
    expected: Moment[Expected],
    actual: Moment[Actual]
  )

  object Departure extends MomentJsonProtocol with StationJsonProtocol with TrainJsonProtocol {
    implicit val departureDecoder: Decoder[Departure] = deriveDecoder
    implicit def departureEntityDecoder[F[_]: Sync]: EntityDecoder[F, Departure] = jsonOf

    implicit val departureEncoder: Encoder[Departure] = deriveEncoder
    implicit def departureEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Departure] =
      jsonEncoderOf

    implicit val showDeparture: Show[Departure] = semiauto.show
  }

  sealed trait DepartureError extends NoStackTrace

  object DepartureError {
    final case class UnexpectedDestination(message: String, destination: Station[Destination])
        extends DepartureError
  }

  def impl[F[_]: Sync: Logger: UUIDGenerator](
    station: Station[Origin],
    connectedStations: NonEmptyList[Station[Destination]],
    eventSender: EventSender[F]
  ): Departures[F] =
    (departure: Departure) =>
      for {
        departed <- connectedStations.find(_ === departure.to) match {
          case Some(_) =>
            F.nextUuid
              .map(EventId.fromUuid)
              .map(eventId =>
                Departed(
                  id = eventId,
                  trainId = departure.trainId,
                  from = station,
                  to = departure.to,
                  expected = departure.expected,
                  created = departure.actual.asMoment[Created]
                )
              )
              .flatTap(eventSender.send)
          case None =>
            F.error(
              show"Tried to create departure to an unreachable station: $departure"
            ) *> UnexpectedDestination("Destination is not connected to this station", departure.to)
              .raiseError[F, Departed]
        }
      } yield departed
}
