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

import cats.Show
import cats.derived.semiauto
import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.typelevel.log4cats.Logger

import scala.util.control.NoStackTrace

trait Arrivals[F[_]] {
  def register(arrival: Arrival): F[Arrived]
}

object Arrivals {
  final case class Arrival(trainId: TrainId, actual: Moment[Actual])

  object Arrival extends MomentJsonProtocol with TrainJsonProtocol {
    implicit val arrivalDecoder: Decoder[Arrival] = deriveDecoder
//    implicit def arrivalEntityDecoder[F[_]: Async]: EntityDecoder[F, Arrival] = jsonOf

    implicit val arrivalEncoder: Encoder[Arrival] = deriveEncoder
//    implicit def arrivalEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Arrival] = jsonEncoderOf

    implicit val showArrival: Show[Arrival] = semiauto.show
  }

  sealed trait ArrivalError extends NoStackTrace

  object ArrivalError extends TrainJsonProtocol {
    final case class UnexpectedTrain(message: String, trainId: TrainId) extends ArrivalError

    implicit val unexpectedTrainDecoder: Decoder[UnexpectedTrain] = deriveDecoder

    implicit val unexpectedTrainEncoder: Encoder[UnexpectedTrain] = deriveEncoder[UnexpectedTrain]

    @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
    implicit val arrivalErrorDecoder: Decoder[ArrivalError] =
      List[Decoder[ArrivalError]](Decoder[UnexpectedTrain].widen).reduceLeft(_ or _)

    implicit val arrivalErrorEncoder: Encoder[ArrivalError] = Encoder.instance {
      case unexpectedTrain @ UnexpectedTrain(_, _) => unexpectedTrain.asJson
    }
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
                  created = arrival.actual.as[Created]
                )
              )
              .flatTap(arrived => expectedTrains.removeAllIdentifiedBy(arrived.trainId))
              .flatTap(eventSender.send)
          case None =>
            F.error(
              show"Tried to create arrival of an unexpected train: $arrival"
            ) *> UnexpectedTrain("There is no recorded departure for the train", arrival.trainId)
              .raiseError[F, Arrived]
        }
      } yield arrived
}
