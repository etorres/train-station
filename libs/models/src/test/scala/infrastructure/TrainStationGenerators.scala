package es.eriktorr.train_station
package infrastructure

import event.EventId
import time.Moment
import time.Moment.When.Actual
import train.TrainId

import org.scalacheck._

import java.time.Instant

object TrainStationGenerators extends TimeGenerators {
  val actualGen: Gen[Moment[Actual]] = Arbitrary.arbitrary[Instant].map(Moment[Actual])

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val eventIdGen: Gen[EventId] =
    Gen.uuid.map(uuid => EventId.fromString(uuid.toString).toOption.get)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val trainIdGen: Gen[TrainId] = Gen.identifier.map(TrainId.fromString(_).toOption.get)
}
