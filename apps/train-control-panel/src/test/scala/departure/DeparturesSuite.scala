package es.eriktorr.train_station
package departure

import departure.Departures.Departure
import effect._
import event.Event.Departed
import event.{Event, EventId}
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
import time.Moment.When.{Actual, Created, Expected}
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
      connectedStations: NonEmptyList[Station[Destination]],
      departure: Departure,
      eventId: EventId,
      expectedEvents: List[Event]
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    val gen = for {
      (origin, allDestinations) <- nDistinct(4, stationGen[Destination]).map {
        _.splitAt(1) match {
          case (xs, ys) =>
            (
              NonEmptyList.fromListUnsafe(xs.map(_.asStation[StationOrigin])).head,
              NonEmptyList.fromListUnsafe(ys)
            )
        }
      }
      trainId <- trainIdGen
      (destination, otherDestinations) = allDestinations.splitAt(1) match {
        case (xs, ys) => (NonEmptyList.fromListUnsafe(xs).head, NonEmptyList.fromListUnsafe(ys))
      }
      actual <- momentGen[Actual]
      expected <- afterGen(actual).map(_.asMoment[Expected])
      eventId <- eventIdGen
      (connectedStations, expectedEvents) <- Gen.oneOf(true, false).map {
        if (_)
          (
            allDestinations,
            List(
              Departed(
                id = eventId,
                trainId = trainId,
                from = origin,
                to = destination,
                expected = expected,
                created = actual.asMoment[Created]
              )
            )
          )
        else (otherDestinations, List.empty[Event])
      }
    } yield TestCase(
      origin,
      connectedStations,
      Departure(trainId, destination, expected, actual),
      eventId,
      expectedEvents
    )

    def checkDeparture[A](
      departure: Departure,
      httpRoutes: HttpRoutes[IO],
      expectedStatus: Status,
      expectedBody: Option[A]
    )(implicit ev: EntityDecoder[IO, A]) = check(
      httpRoutes = httpRoutes,
      request = Request(
        method = Method.POST,
        uri = uri"departure",
        headers = Headers.of(`Content-Type`(MediaType.application.json)),
        body = Departure
          .departureEntityEncoder[IO]
          .toEntity(departure)
          .body
      ),
      expectedStatus = expectedStatus,
      expectedBody = expectedBody
    )

    forall(gen) {
      case TestCase(origin, connectedStations, departure, eventId, expectedEvents) =>
        for {
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          eventSenderStateRef <- EventSenderState.refEmpty
          implicit0(logger: Logger[IO]) <- Slf4jLogger.create[F]
          implicit0(uuidGenerator: UUIDGenerator[IO]) = FakeUUIDGenerator.impl[IO](
            uuidGeneratorStateRef
          )
          httpExpectations <- {
            val httpRoutes = AllHttpRoutes.departureRoutes[IO](
              Departures
                .impl[IO](origin, connectedStations, FakeEventSender.impl[IO](eventSenderStateRef))
            )
            if (connectedStations.contains_(departure.to))
              checkDeparture(departure, httpRoutes, Status.Created, eventId.some)
            else
              checkDeparture(
                departure,
                httpRoutes,
                Status.BadRequest,
                s"Unexpected destination ${departure.to.unStation.value}".some
              )
          }
          sentEvents <- eventSenderStateRef.get
        } yield httpExpectations && expect(sentEvents.events === expectedEvents)
    }
  }
}
