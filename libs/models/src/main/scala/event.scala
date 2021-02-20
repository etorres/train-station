package es.eriktorr

import effect._
import error._
import station._
import time._
import train._

import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.predicates.all._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object event {
  @newtype class EventId(val unEventId: Uuid)

  object EventId {
    def fromString(str: String): Either[InvalidParameter, EventId] = refineV[Uuid](str) match {
      case Left(_) => InvalidParameter("Event Id should be a valid UUID").asLeft
      case Right(refinedStr) => refinedStr.coerce[EventId].asRight
    }
  }

  sealed trait Event {
    def id: EventId
    def trainId: TrainId
    def created: Moment[Created]
  }

  final case class Departed(
    id: EventId,
    trainId: TrainId,
    from: Station[Origin],
    to: Station[Destination],
    expected: Moment[Expected],
    created: Moment[Created]
  ) extends Event

  final case class Arrived(
    id: EventId,
    trainId: TrainId,
    from: Station[Origin],
    to: Station[Destination],
    expected: Moment[Expected],
    created: Moment[Created]
  ) extends Event
}
