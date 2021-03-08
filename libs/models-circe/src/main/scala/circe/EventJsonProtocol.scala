package es.eriktorr.train_station
package circe

import event.EventId

import io.circe._

trait EventJsonProtocol extends StringFieldDecoder {
  implicit val eventIdDecoder: Decoder[EventId] = decodeValue[EventId](EventId.fromString)

  implicit val eventIdEncoder: Encoder[EventId] = (eventId: EventId) =>
    Json.fromString(eventId.unEventId.value)
}
