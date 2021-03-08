package es.eriktorr.train_station
package departure

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

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.typelevel.log4cats.Logger

trait Departures[F[_]] {
  import Departures.{Departure, DepartureError}

  def register(departure: Departure): F[Either[DepartureError, Departed]]
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
  }

  sealed trait DepartureError

  object DepartureError {
    final case class UnexpectedDestination(station: Station[Destination]) extends DepartureError
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
            F.next
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
              .map(_.asRight)
          case None =>
            val error = DepartureError.UnexpectedDestination(departure.to)
            F.error(
                s"Tried to create departure to an unexpected destination: ${departure.toString}"
              )
              .as(error.asLeft)
        }
      } yield departed
}
