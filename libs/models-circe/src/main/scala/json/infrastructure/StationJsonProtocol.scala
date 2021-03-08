package es.eriktorr.train_station
package json.infrastructure

import station.Station

import io.circe._

trait StationJsonProtocol extends StringFieldDecoder {
  implicit def stationDecoder[A <: Station.TravelDirection]: Decoder[Station[A]] =
    decode[Station[A]]("station", Station.fromString[A])

  implicit def stationEncoder[A <: Station.TravelDirection]: Encoder[Station[A]] =
    (station: Station[A]) => Json.obj(("station", Json.fromString(station.unStation.toString)))
}
