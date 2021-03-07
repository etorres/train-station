package es.eriktorr.train_station
package arrival

import arrival.ExpectedTrains.ExpectedTrain
import station.Station
import station.Station.TravelDirection.Origin
import time.Moment
import time.Moment.When.Expected
import train.TrainId

import cats.derived.semiauto
import cats.effect.Sync
import cats.{Eq, Show}

trait ExpectedTrains[F[_]] {
  def findBy(trainId: TrainId): F[Option[ExpectedTrain]]
  def removeAllIdentifiedBy(trainId: TrainId): F[Unit]
  def update(expectedTrain: ExpectedTrain): F[Unit]
}

object ExpectedTrains {
  final case class ExpectedTrain(
    trainId: TrainId,
    from: Station[Origin],
    expected: Moment[Expected]
  )

  object ExpectedTrain {
    implicit val eqExpectedTrain: Eq[ExpectedTrain] = semiauto.eq
    implicit val showExpectedTrain: Show[ExpectedTrain] = semiauto.show
  }

  def impl[F[_]: Sync]: ExpectedTrains[F] = ???
}
