package es.eriktorr.train_station
package arrival

import arrival.Arrivals.Arrival
import arrival.ExpectedTrains.ExpectedTrain
import arrival.infrastructure.FakeExpectedTrains
import arrival.infrastructure.FakeExpectedTrains.ExpectedTrainsState
import event.Event.Arrived
import event.EventId
import http.infrastructure.AllHttpRoutes
import json.infrastructure.EventJsonProtocol
import messaging.infrastructure.FakeEventSender
import messaging.infrastructure.FakeEventSender.EventSenderState
import shared.infrastructure.Generators.nDistinct
import shared.infrastructure.TrainControlPanelGenerators.expectedTrainGen
import shared.infrastructure.TrainStationGenerators._
import spec.HttpRoutesIOCheckers
import station.Station
import station.Station.TravelDirection.{Destination, Origin => StationOrigin}
import time.Moment
import time.Moment.When.{Actual, Created}
import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.Show
import cats.data.NonEmptyList
import cats.derived._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.headers._
import org.http4s.implicits._
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._
import weaver.scalacheck._

object ArrivalsSuite
    extends SimpleIOSuite
    with Checkers
    with HttpRoutesIOCheckers
    with EventJsonProtocol {

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
          logger <- Slf4jLogger.create[IO]
          expectedTrainsRef <- ExpectedTrainsState.refFrom(expectedTrains.toList)
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          eventSenderStateRef <- EventSenderState.refEmpty
          train = expectedTrains.head
          httpExpectations <- {
            implicit val testLogger: Logger[IO] = logger
            implicit val uuidGenerator: UUIDGenerator[IO] =
              FakeUUIDGenerator.impl[IO](uuidGeneratorStateRef)
            check(
              httpRoutes = AllHttpRoutes.arrivalRoutes[IO](
                Arrivals.impl[IO](
                  destination,
                  FakeExpectedTrains.impl[IO](expectedTrainsRef),
                  FakeEventSender.impl[IO](eventSenderStateRef)
                )
              ),
              request = Request(
                method = Method.POST,
                uri = uri"arrival",
                headers = Headers.of(`Content-Type`(MediaType.application.json)),
                body = Arrival
                  .arrivalEntityEncoder[IO]
                  .toEntity(Arrival(train.trainId, actual))
                  .body
              ),
              expectedStatus = Status.Created,
              expectedBody = eventId.some
            )
          }
          sentEvents <- eventSenderStateRef.get
        } yield httpExpectations && expect(
          sentEvents.events === List(
            Arrived(
              id = eventId,
              trainId = train.trainId,
              from = train.from,
              to = destination,
              expected = train.expected,
              created = actual.asMoment[Created]
            )
          )
        )
    }
  }
}
