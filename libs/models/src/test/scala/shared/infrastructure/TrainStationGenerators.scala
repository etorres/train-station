package es.eriktorr.train_station
package shared.infrastructure

import event.EventId
import station.Station
import time.Moment
import train.TrainId

import org.scalacheck._

import java.time.OffsetDateTime

object TrainStationGenerators extends TimeGenerators {
  def afterGen[A <: Moment.When](moment: Moment[A]): Gen[Moment[A]] =
    Gen.choose(1L, 480L).map(minutes => Moment(moment.unMoment.plusMinutes(minutes)))

  val eventIdGen: Gen[EventId] = Gen.uuid.map(uuid => EventId.fromUuid(uuid))

  def momentGen[A <: Moment.When]: Gen[Moment[A]] =
    Arbitrary.arbitrary[OffsetDateTime].map(Moment[A])

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def stationGen[A <: Station.TravelDirection]: Gen[Station[A]] =
    for {
      length <- Gen.choose(3, 10)
      station <- Gen
        .listOfN[Char](length, Gen.alphaChar)
        .map(xs => Station.fromString[A](xs.mkString).toOption.get)
    } yield station

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val trainIdGen: Gen[TrainId] =
    Gen.uuid.map(uuid => TrainId.fromString(uuid.toString).toOption.get)
}
