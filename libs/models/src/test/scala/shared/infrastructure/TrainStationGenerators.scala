package es.eriktorr.train_station
package shared.infrastructure

import event.EventId
import station.Station
import time.Moment
import train.TrainId

import org.scalacheck._

import java.time.Instant

object TrainStationGenerators extends TimeGenerators {
  def afterGen[A <: Moment.When](moment: Moment[A]): Gen[Moment[A]] =
    Gen.choose(1, 480).map(minutes => Moment(moment.unMoment.plusMillis(minutes * 1000L)))

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val eventIdGen: Gen[EventId] =
    Gen.uuid.map(uuid => EventId.fromString(uuid.toString).toOption.get)

  def momentGen[A <: Moment.When]: Gen[Moment[A]] = Arbitrary.arbitrary[Instant].map(Moment[A])

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def stationGen[A <: Station.TravelDirection]: Gen[Station[A]] =
    for {
      length <- Gen.choose(3, 10)
      station <- Gen
        .listOfN[Char](length, Gen.alphaChar)
        .map(xs => Station.fromString[A](xs.mkString).toOption.get)
    } yield station

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val trainIdGen: Gen[TrainId] = Gen.identifier.map(TrainId.fromString(_).toOption.get)
}
