package es.eriktorr

import event.EventId
import time.Moment
import train.TrainId

import cats.implicits._
import io.circe._

import java.time.Instant
import scala.util.Try

object circe {
  trait EventJsonProtocol {
    implicit val eventIdDecoder: Decoder[EventId] =
      fieldDecoder[EventId]("eventId", EventId.fromString)

    implicit val eventIdEncoder: Encoder[EventId] = (eventId: EventId) =>
      Json.obj(("eventId", Json.fromString(eventId.unEventId.value)))
  }

  trait TrainJsonProtocol {
    implicit val trainIdDecoder: Decoder[TrainId] =
      fieldDecoder[TrainId]("trainId", TrainId.fromString)

    implicit val trainIdEncoder: Encoder[TrainId] = (trainId: TrainId) =>
      Json.obj(("trainId", Json.fromString(trainId.unTrainId.value)))
  }

  trait MomentJsonProtocol {
    implicit def momentDecoder[A <: Moment.When]: Decoder[Moment[A]] =
      fieldDecoder[Moment[A]]("moment", str => Try(Instant.parse(str)).map(Moment[A]).toEither)

    implicit def momentEncoder[A <: Moment.When]: Encoder[Moment[A]] =
      (moment: Moment[A]) => Json.obj(("moment", Json.fromString(moment.unMoment.toString)))
  }

  private[this] def fieldDecoder[A](field: String, fA: String => Either[_ <: Throwable, A]) =
    new Decoder[A] {
      override def apply(cursor: HCursor): Decoder.Result[A] =
        cursor
          .downField(field)
          .as[String]
          .map(fA)
          .fold(
            _.asLeft, {
              case Left(constructorError) =>
                DecodingFailure(
                  s"Failed to decode: ${cursor.value.toString}, with error: ${constructorError.getMessage}",
                  List.empty
                ).asLeft
              case Right(value) => value.asRight
            }
          )
    }
}
