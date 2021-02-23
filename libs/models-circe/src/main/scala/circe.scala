package es.eriktorr

import event.EventId
import time.Moment
import train.TrainId

import cats.implicits._
import io.circe._

import java.time.Instant
import scala.util.{Failure, Success, Try}

object circe {
  trait EventJsonProtocol {
    implicit val eventIdDecoder: Decoder[EventId] = (cursor: HCursor) =>
      cursor
        .downField("eventId")
        .as[String]
        .map(EventId.fromString)
        .fold(
          _.asLeft, {
            case Left(constructorError) =>
              DecodingFailure(
                s"Failed to decode: ${cursor.value.toString}, with error: ${constructorError.error}",
                List.empty
              ).asLeft
            case Right(eventId) => eventId.asRight
          }
        )

    implicit val eventIdEncoder: Encoder[EventId] = (eventId: EventId) =>
      Json.obj(("eventId", Json.fromString(eventId.unEventId.value)))
  }

  trait TrainJsonProtocol {
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
  }

  trait MomentJsonProtocol {
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
  }
}
