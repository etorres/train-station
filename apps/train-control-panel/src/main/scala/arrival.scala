package es.eriktorr

import event.Event.Arrived
import event.EventId
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment
import time.Moment.When.{Actual, Created, Expected}
import train.TrainId

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._

import java.time.Instant
import scala.util.{Failure, Success, Try}

object arrival {
  final case class Arrival(trainId: TrainId, actual: Moment[Actual])

  object Arrival {
    implicit val trainIdDecoder: Decoder[TrainId] = (cursor: HCursor) =>
      cursor
        .downField("trainId")
        .as[String]
        .map(TrainId.fromString)
        .fold(
          _.asLeft, {
            case Left(constructorError) =>
              DecodingFailure(
                s"Failed to decode: ${cursor.value.toString}, with error: ${constructorError.error}",
                List.empty
              ).asLeft
            case Right(trainId) => trainId.asRight
          }
        )

    implicit val trainIdEncoder: Encoder[TrainId] = (trainId: TrainId) =>
      Json.obj(("trainId", Json.fromString(trainId.unTrainId.value)))

    implicit def momentEncoder[A <: Moment.When]: Encoder[Moment[A]] =
      (moment: Moment[A]) => Json.obj(("moment", Json.fromString(moment.unMoment.toString)))

    implicit def momentDecoder[A <: Moment.When]: Decoder[Moment[A]] =
      (cursor: HCursor) =>
        cursor
          .downField("moment")
          .as[String]
          .map(str => Try(Instant.parse(str)))
          .fold(
            _.asLeft,
            a =>
              a match {
                case Failure(exception) =>
                  DecodingFailure(
                    s"Failed to decode: ${cursor.value.toString}, with error: ${exception.getMessage}",
                    List()
                  ).asLeft
                case Success(instant) => Moment[A](instant).asRight
              }
          )

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
    def impl[F[_]: Sync]: Arrivals[F] = new Arrivals[F] {
      override def register(arrival: Arrival): F[Either[ArrivalError, Arrived]] =
        for {
          id <- F.fromEither(EventId.fromString("8ad325ab-c42b-47ae-8018-cb11a8aa80f6"))
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
