package es.eriktorr.train_station

import error.TrainStationError.InvalidParameter
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment
import time.Moment.When.{Created, Expected}
import train.TrainId

import cats.derived._
import cats.implicits._
import cats.{Eq, Show}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.predicates.all.Uuid
import eu.timepit.refined.refineV
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import java.util.UUID

object event {
  @newtype class EventId(val unEventId: String Refined Uuid)

  object EventId {
    def fromString(str: String): Either[InvalidParameter, EventId] = refineV[Uuid](str) match {
      case Left(_) => InvalidParameter("Event Id should be a valid UUID").asLeft
      case Right(refinedStr) => refinedStr.coerce[EventId].asRight
    }

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def fromUuid(uuid: UUID): EventId = EventId.fromString(uuid.toString).toOption.get

    implicit def eqEventId: Eq[EventId] = Eq.fromUniversalEquals
    implicit val showEventId: Show[EventId] = Show.show(_.toString)
  }

  sealed trait Event {
    def id: EventId
    def trainId: TrainId
    def created: Moment[Created]
  }

  object Event {
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

    implicit val eqEvent: Eq[Event] = semiauto.eq
  }
}
