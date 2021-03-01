package es.eriktorr.train_station
package arrival

import arrival.ExpectedTrains.ExpectedTrain
import arrival.FakeExpectedTrains.ExpectedTrainsState
import train.TrainId

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._

final class FakeExpectedTrains[F[_]: Sync] private[arrival] (val ref: Ref[F, ExpectedTrainsState])
    extends ExpectedTrains[F] {
  override def findBy(trainId: TrainId): F[Option[ExpectedTrain]] =
    ref.get.map(_.trains.find(_.trainId == trainId))

  override def removeAllIdentifiedBy(trainId: TrainId): F[Unit] = ???

  override def update(expectedTrain: ExpectedTrain): F[Unit] = ???
}

object FakeExpectedTrains {
  final case class ExpectedTrainsState(trains: List[ExpectedTrain])

  object ExpectedTrainsState {
    def refFrom[F[_]: Sync](trains: List[ExpectedTrain]): F[Ref[F, ExpectedTrainsState]] =
      Ref.of[F, ExpectedTrainsState](ExpectedTrainsState(trains))
  }

  def impl[F[_]: Sync](ref: Ref[F, ExpectedTrainsState]) = new FakeExpectedTrains[F](ref)
}
