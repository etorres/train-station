package es.eriktorr.train_station
package vulcan

import station.Station

import _root_.vulcan._
import cats.implicits._

trait StationAvroCodec {
  implicit def stationCodec[A <: Station.TravelDirection]: Codec[Station[A]] =
    Codec.string.imapError(value =>
      Station.fromString[A](value) match {
        case Left(constructorError) => AvroError(constructorError.error).asLeft
        case Right(station) => station.asRight
      }
    )(_.unStation.value)
}
