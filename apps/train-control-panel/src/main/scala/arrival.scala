package es.eriktorr.train_station

import circe.{MomentJsonProtocol, TrainJsonProtocol}
import event.Event.Arrived
import event.EventId
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment
import time.Moment.When.{Actual, Created, Expected}
import train.TrainId
import uuid.UUIDGenerator

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._

object arrival {
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

  trait Arrivals[F[_]] {
    def register(arrival: Arrival): F[Either[ArrivalError, Arrived]]
  }

  object Arrivals {
    def impl[F[_]: Sync: UUIDGenerator]: Arrivals[F] = new Arrivals[F] {
      override def register(arrival: Arrival): F[Either[ArrivalError, Arrived]] =
        for {
          uuid <- UUIDGenerator[F].next.map(_.toString)
          id <- F.fromEither(EventId.fromString(uuid))
          origin <- F.fromEither(Station.fromString[Origin]("Barcelona"))
          destination <- F.fromEither(Station.fromString[Destination]("Girona"))
        } yield Arrived(
          id = id,
          trainId = arrival.trainId,
          from = origin,
          to = destination,
          expected = arrival.actual.asMoment[Expected],
          created = arrival.actual.asMoment[Created]
        ).asRight
    }
  }
}
