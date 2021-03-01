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

  override def removeAllIdentifiedBy(trainId: TrainId): F[Unit] =
    ref.get.flatMap(current =>
      ref.set(current.copy(current.trains.filterNot(_.trainId == trainId)))
    )

  override def update(expectedTrain: ExpectedTrain): F[Unit] = ref.get.map { current =>
    val _ = ref.set(current.copy(expectedTrain :: current.trains)); ()
  }
}

object FakeExpectedTrains {
  final case class ExpectedTrainsState(trains: List[ExpectedTrain])

  object ExpectedTrainsState {
    def refFrom[F[_]: Sync](trains: List[ExpectedTrain]): F[Ref[F, ExpectedTrainsState]] =
      Ref.of[F, ExpectedTrainsState](ExpectedTrainsState(trains))
  }

  def impl[F[_]: Sync](ref: Ref[F, ExpectedTrainsState]): FakeExpectedTrains[F] =
    new FakeExpectedTrains[F](ref)
}
