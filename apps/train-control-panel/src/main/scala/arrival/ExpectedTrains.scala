package es.eriktorr.train_station
package arrival

import arrival.ExpectedTrains.ExpectedTrain
import station.Station
import station.Station.TravelDirection.Origin
import time.Moment
import time.Moment.When.Expected
import train.TrainId

import cats.Show

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

  implicit val showExpectedTrains: Show[ExpectedTrain] = Show.show(_.toString)
}
