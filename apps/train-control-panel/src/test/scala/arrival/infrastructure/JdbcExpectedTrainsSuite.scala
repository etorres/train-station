package es.eriktorr.train_station
package arrival.infrastructure

import arrival.ExpectedTrains.ExpectedTrain
import shared.infrastructure.TrainStationGenerators.{momentGen, stationGen, trainIdGen}
import spec.JdbcIOSuiteWithCheckers
import station.Station.TravelDirection.Origin
import time.Moment.When.Expected

import cats.Show
import cats.derived._
import cats.effect.IO
import cats.implicits._

object JdbcExpectedTrainsSuite extends JdbcIOSuiteWithCheckers {
  override def currentSchema: String = "test_expected_trains"

  test("insert, find and delete expected train by Id") {
    final case class TestCase(expectedTrain: ExpectedTrain)

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    val gen = for {
      trainId <- trainIdGen
      origin <- stationGen[Origin]
      expected <- momentGen[Expected]
    } yield TestCase(ExpectedTrain(trainId, origin, expected))

    forall(gen) {
      case TestCase(expectedTrain) =>
        testResources.use { transactor =>
          val expectedTrains = JdbcExpectedTrains.impl[IO](transactor)
          for {
            _ <- expectedTrains.update(expectedTrain)
            created <- expectedTrains.findBy(expectedTrain.trainId)
            _ <- expectedTrains.removeAllIdentifiedBy(expectedTrain.trainId)
            deleted <- expectedTrains.findBy(expectedTrain.trainId)
          } yield expect(created === expectedTrain.some) && expect(deleted === none[ExpectedTrain])
        }
    }
  }
}
