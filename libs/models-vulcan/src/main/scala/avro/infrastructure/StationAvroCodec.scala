package es.eriktorr.train_station
package avro.infrastructure

import station.Station

import cats.implicits._
import vulcan._

trait StationAvroCodec {
  implicit def stationCodec[A <: Station.TravelDirection]: Codec[Station[A]] =
    Codec.string.imapError(value =>
      Station.fromString[A](value) match {
        case Left(constructorError) => AvroError(constructorError.error).asLeft
        case Right(station) => station.asRight
      }
    )(_.unStation.value)
}
