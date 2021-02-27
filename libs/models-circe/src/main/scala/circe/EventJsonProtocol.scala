package es.eriktorr.train_station
package circe

import event.EventId

import io.circe._

trait EventJsonProtocol extends StringFieldDecoder {
  implicit val eventIdDecoder: Decoder[EventId] = decode[EventId]("eventId", EventId.fromString)

  implicit val eventIdEncoder: Encoder[EventId] = (eventId: EventId) =>
    Json.obj(("eventId", Json.fromString(eventId.unEventId.value)))
}
