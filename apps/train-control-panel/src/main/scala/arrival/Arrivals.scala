package es.eriktorr.train_station
package arrival

import arrival.Arrivals.Arrival
import arrival.Arrivals.ArrivalError.UnexpectedTrain
import event.Event.Arrived
import event.EventId
import json.infrastructure.{MomentJsonProtocol, TrainJsonProtocol}
import messaging.EventSender
import station.Station
import station.Station.TravelDirection.Destination
import time.Moment
import time.Moment.When.{Actual, Created}
import train.TrainId
import uuid.UUIDGenerator

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

trait Arrivals[F[_]] {
  def register(arrival: Arrival): F[Arrived]
}

object Arrivals {
  final case class Arrival(trainId: TrainId, actual: Moment[Actual])

  object Arrival extends MomentJsonProtocol with TrainJsonProtocol {
    implicit val arrivalDecoder: Decoder[Arrival] = deriveDecoder
    implicit def arrivalEntityDecoder[F[_]: Sync]: EntityDecoder[F, Arrival] = jsonOf

    implicit val arrivalEncoder: Encoder[Arrival] = deriveEncoder
    implicit def arrivalEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Arrival] = jsonEncoderOf

    implicit val showArrival: Show[Arrival] = semiauto.show
  }

  sealed trait ArrivalError extends NoStackTrace

  object ArrivalError {
    final case class UnexpectedTrain(trainId: TrainId) extends ArrivalError
  }

  def impl[F[_]: Sync: Logger: UUIDGenerator](
    station: Station[Destination],
    expectedTrains: ExpectedTrains[F],
    eventSender: EventSender[F]
  ): Arrivals[F] =
    (arrival: Arrival) =>
      for {
        maybeTrain <- expectedTrains.findBy(arrival.trainId)
        arrived <- maybeTrain match {
          case Some(expectedTrain) =>
            F.nextUuid
              .map(EventId.fromUuid)
              .map(eventId =>
                Arrived(
                  id = eventId,
                  trainId = arrival.trainId,
                  from = expectedTrain.from,
                  to = station,
                  expected = expectedTrain.expected,
                  created = arrival.actual.asMoment[Created]
                )
              )
              .flatTap(arrived => expectedTrains.removeAllIdentifiedBy(arrived.trainId))
              .flatTap(eventSender.send)
          case None =>
            F.error(
              show"Tried to create arrival of an unexpected train: $arrival"
            ) *> UnexpectedTrain(arrival.trainId).raiseError[F, Arrived]
        }
      } yield arrived
}
