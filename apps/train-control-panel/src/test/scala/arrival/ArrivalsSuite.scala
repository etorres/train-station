package es.eriktorr.train_station
package arrival

import arrival.Arrivals.Arrival
import arrival.FakeExpectedTrains.ExpectedTrainsState
import circe._
import event.EventId
import shared.infrastructure.Generators.nDistinct
import shared.infrastructure.TrainControlPanelGenerators.expectedTrainGen
import shared.infrastructure.TrainStationGenerators._
import spec.HttpRoutesIOCheckers
import station.Station
import station.Station.TravelDirection.{Destination, Origin => StationOrigin}
import time.Moment
import time.Moment.When.Actual
import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.Show
import cats.data.NonEmptyList
import cats.derived._
import cats.effect._
import cats.implicits._
import arrival.ExpectedTrains.ExpectedTrain
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.headers._
import org.http4s.implicits._
import org.scalacheck.cats.implicits._
import org.scalacheck.Gen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._
import weaver.scalacheck._

object ArrivalsSuite extends SimpleIOSuite with Checkers with HttpRoutesIOCheckers {

  test("create train arrivals") {
    final case class TestCase(
      destination: Station[Destination],
      expectedTrains: NonEmptyList[ExpectedTrain],
      actual: Moment[Actual],
      eventId: EventId
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    val gen = for {
      destination <- stationGen[Destination]
      trainIds <- nDistinct(3, trainIdGen)
      origins <- nDistinct(2, stationGen[StationOrigin])
      expectedTrains <- trainIds.traverse(trainId => expectedTrainGen(trainId, Gen.oneOf(origins)))
      actual <- momentGen[Actual]
      eventId <- eventIdGen
    } yield TestCase(destination, NonEmptyList.fromListUnsafe(expectedTrains), actual, eventId)

    forall(gen) {
      case TestCase(destination, expectedTrains, actual, eventId) =>
        for {
          logger <- Slf4jLogger.create[F]
          expectedTrainsRef <- ExpectedTrainsState.refFrom(expectedTrains.toList)
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          expectations <- {
            implicit val testLogger: Logger[F] = logger
            implicit val uuidGenerator: UUIDGenerator[IO] =
              FakeUUIDGenerator.impl[IO](uuidGeneratorStateRef)
            check(
              httpRoutes = TrainControlPanelRoutes.arrivalRoutes[IO](
                Arrivals.impl[IO](destination, FakeExpectedTrains.impl[IO](expectedTrainsRef), ???)
              ),
              request = Request(
                method = Method.POST,
                uri = uri"arrival",
                headers = Headers.of(`Content-Type`(MediaType.application.json)),
                body = Arrival
                  .arrivalEntityEncoder[IO]
                  .toEntity(Arrival(expectedTrains.head.trainId, actual))
                  .body
              ),
              expectedStatus = Status.Created,
              expectedBody = eventId.some
            )
          }
        } yield expectations
    }
  }
}
