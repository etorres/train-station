package es.eriktorr.train_station
package avro.infrastructure

import event.Event.{Arrived, Departed}
import event.{Event, EventId}

import cats.implicits._
import vulcan._

trait EventAvroCodec extends MomentAvroCodec with TrainAvroCodec with StationAvroCodec {
  implicit val eventIdCodec: Codec[EventId] = Codec.string.imapError(value =>
    EventId.fromString(value) match {
      case Left(constructorError) => AvroError(constructorError.error).asLeft
      case Right(eventId) => eventId.asRight
    }
  )(_.unEventId.value)

  implicit val departedCodec: Codec[Departed] =
    Codec.record(
      name = "Departed",
      namespace = "es.eriktorr.train_station.event"
    ) { field =>
      (
        field("id", _.id),
        field("trainId", _.trainId),
        field("from", _.from),
        field("to", _.to),
        field("expected", _.expected),
        field("created", _.created)
      ).mapN(Departed)
    }

  implicit val arrivedCodec: Codec[Arrived] =
    Codec.record(
      name = "Arrived",
      namespace = "es.eriktorr.train_station.event"
    ) { field =>
      (
        field("id", _.id),
        field("trainId", _.trainId),
        field("from", _.from),
        field("to", _.to),
        field("expected", _.expected),
        field("created", _.created)
      ).mapN(Arrived)
    }

  implicit val eventCodec: Codec[Event] = Codec.union[Event](alt => alt[Departed] |+| alt[Arrived])
}
