package es.eriktorr.train_station
package arrival.infrastructure

import arrival.ExpectedTrains
import arrival.ExpectedTrains.ExpectedTrain
import doobie._
import station.Station
import station.Station.TravelDirection.Origin
import time.Moment
import time.Moment.When.Expected
import train.TrainId

import _root_.doobie._
import _root_.doobie.implicits._
import cats.effect._
import cats.implicits._

final class JdbcExpectedTrains[F[_]: Async] private (transactor: Transactor[F])
    extends ExpectedTrains[F]
    with StationMapping
    with TimeMapping
    with TrainMapping {
  implicit val expectedTrainRead: Read[ExpectedTrain] =
    Read[(TrainId, Station[Origin], Moment[Expected])].map {
      case (trainId, from, expected) => ExpectedTrain(trainId, from, expected)
    }

  override def findBy(trainId: TrainId): F[Option[ExpectedTrain]] =
    sql"""
         SELECT 
           train_id,
           origin, 
           expected
         FROM expected_trains
         WHERE
           train_id = $trainId"""
      .query[ExpectedTrain]
      .option
      .transact(transactor)

  override def removeAllIdentifiedBy(trainId: TrainId): F[Unit] = F.unit <* sql"""
      DELETE FROM expected_trains WHERE train_id = $trainId
       """.update.run.transact(transactor)

  override def update(expectedTrain: ExpectedTrain): F[Unit] = F.unit <* sql"""
        INSERT INTO 
          expected_trains (train_id, origin, expected) 
        VALUES (
          ${expectedTrain.trainId}, 
          ${expectedTrain.from},
          ${expectedTrain.expected}
        )
        ON CONFLICT (train_id) 
        DO
          UPDATE SET origin = EXCLUDED.origin, expected = EXCLUDED.expected
        """.update.run.transact(transactor)
}

object JdbcExpectedTrains {
  def impl[F[_]: Async](transactor: Transactor[F]) = new JdbcExpectedTrains[F](transactor)
}
