package es.eriktorr.train_station
package arrival

import arrival.Arrivals.{Arrival, ArrivalError}
import circe.{MomentJsonProtocol, TrainJsonProtocol}
import event.Event.Arrived
import event.EventId
import notification.EventSender
import station.Station
import station.Station.TravelDirection.Destination
import time.Moment
import time.Moment.When.{Actual, Created}
import train.TrainId
import uuid.UUIDGenerator

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.typelevel.log4cats.Logger

trait Arrivals[F[_]] {
  def register(arrival: Arrival): F[Either[ArrivalError, Arrived]]
}

object Arrivals {
  final case class Arrival(trainId: TrainId, actual: Moment[Actual])

  object Arrival extends MomentJsonProtocol with TrainJsonProtocol {
    implicit val arrivalDecoder: Decoder[Arrival] = deriveDecoder
    implicit def arrivalEntityDecoder[F[_]: Sync]: EntityDecoder[F, Arrival] = jsonOf

    implicit val arrivalEncoder: Encoder[Arrival] = deriveEncoder
    implicit def arrivalEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Arrival] = jsonEncoderOf
  }

  sealed trait ArrivalError

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
            F.next
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
              .flatTap(arrived => eventSender.send(arrived))
              .map(_.asRight)
          case None =>
            val error = ArrivalError.UnexpectedTrain(arrival.trainId)
            F.error(s"Tried to create arrival of an unexpected train: ${arrival.toString}")
              .as(error.asLeft)
        }
      } yield arrived
}
