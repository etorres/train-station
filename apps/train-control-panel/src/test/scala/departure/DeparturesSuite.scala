package es.eriktorr.train_station
package departure

import departure.Departures.Departure
import event.Event.Departed
import event.EventId
import http.infrastructure.AllHttpRoutes
import json.infrastructure.EventJsonProtocol
import messaging.infrastructure.FakeEventSender
import messaging.infrastructure.FakeEventSender.EventSenderState
import shared.infrastructure.Generators.nDistinct
import shared.infrastructure.TrainStationGenerators.{
  afterGen,
  eventIdGen,
  momentGen,
  stationGen,
  trainIdGen
}
import spec.HttpRoutesIOCheckers
import station.Station
import station.Station.TravelDirection.{Destination, Origin => StationOrigin}
import time.Moment
import time.Moment.When.{Actual, Created, Expected}
import train.TrainId
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
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._
import weaver.scalacheck._

object DeparturesSuite
    extends SimpleIOSuite
    with Checkers
    with HttpRoutesIOCheckers
    with EventJsonProtocol {

  test("create train departures") {
    final case class TestCase(
      origin: Station[StationOrigin],
      destinations: NonEmptyList[Station[Destination]],
      trainId: TrainId,
      actual: Moment[Actual],
      expected: Moment[Expected],
      eventId: EventId
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    val gen = for {
      origin <- stationGen[StationOrigin]
      destinations <- nDistinct(3, stationGen[Destination])
      trainId <- trainIdGen
      actual <- momentGen[Actual]
      expected <- afterGen(actual).map(_.asMoment[Expected])
      eventId <- eventIdGen
    } yield TestCase(
      origin,
      NonEmptyList.fromListUnsafe(destinations),
      trainId,
      actual,
      expected,
      eventId
    )

    forall(gen) {
      case TestCase(origin, destinations, trainId, actual, expected, eventId) =>
        for {
          logger <- Slf4jLogger.create[F]
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          eventSenderStateRef <- EventSenderState.refEmpty
          httpExpectations <- {
            implicit val testLogger: Logger[F] = logger
            implicit val uuidGenerator: UUIDGenerator[IO] =
              FakeUUIDGenerator.impl[IO](uuidGeneratorStateRef)
            check(
              httpRoutes = AllHttpRoutes.departureRoutes[IO](
                Departures
                  .impl[IO](origin, destinations, FakeEventSender.impl[IO](eventSenderStateRef))
              ),
              request = Request(
                method = Method.POST,
                uri = uri"departure",
                headers = Headers.of(`Content-Type`(MediaType.application.json)),
                body = Departure
                  .departureEntityEncoder[IO]
                  .toEntity(Departure(trainId, destinations.head, expected, actual))
                  .body
              ),
              expectedStatus = Status.Created,
              expectedBody = eventId.some
            )
          }
          sentEvents <- eventSenderStateRef.get
        } yield httpExpectations && expect(
          sentEvents.events === List(
            Departed(
              id = eventId,
              trainId = trainId,
              from = origin,
              to = destinations.head,
              expected = expected,
              created = actual.asMoment[Created]
            )
          )
        )
    }
  }
}
