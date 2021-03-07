package es.eriktorr.train_station
package doobie

import station.Station

import _root_.doobie._

trait StationMapping {
  implicit def stationPut[A <: Station.TravelDirection]: Put[Station[A]] =
    Put[String].contramap(_.unStation.value)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit def stationRead[A <: Station.TravelDirection]: Read[Station[A]] =
    Read[String].map(Station.fromString[A](_).toOption.get)
}
