package es.eriktorr.train_station
package departure

import arrival.infrastructure.FakeArrivals
import departure.Departures.Departure
import departure.Departures.DepartureError.UnexpectedDestination
import effect._
import event.Event.Departed
import event.{Event, EventId}
import http.infrastructure.B3Headers.toHeaders
import http.infrastructure.{B3Headers, HttpServer}
import json.infrastructure.{EventJsonProtocol, StationJsonProtocol}
import messaging.infrastructure.FakeEventSender
import messaging.infrastructure.FakeEventSender.EventSenderState
import shared.infrastructure.Generators.nDistinct
import shared.infrastructure.TraceGenerators.b3Gen
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
import trace.TraceEntryPoint
import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.Show
import cats.data.NonEmptyList
import cats.derived._
import cats.effect._
import cats.implicits._
import io.janstenpickle.trace4cats.inject.Trace
import io.janstenpickle.trace4cats.model.TraceProcess
import org.http4s.MediaType.application
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.jsonEncoderOf
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
    with EventJsonProtocol
    with StationJsonProtocol {

  test("create train departures") {
    final case class TestCase(
      origin: Station[StationOrigin],
      connectedStations: NonEmptyList[Station[Destination]],
      departure: Departure,
      eventId: EventId,
      expectedEvents: List[Event],
      b3Headers: Option[B3Headers]
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
    val gen = for {
      (origin, allDestinations) <- nDistinct(4, stationGen[Destination]).map {
        _.splitAt(1) match {
          case (xs, ys) =>
            (xs.map(_.as[StationOrigin]).head, NonEmptyList.fromListUnsafe(ys))
        }
      }
      trainId <- trainIdGen
      (destination, otherDestinations) = allDestinations.splitAt(1) match {
        case (xs, ys) => (xs.head, NonEmptyList.fromListUnsafe(ys))
      }
      actual <- momentGen[Actual]
      expected <- afterGen(actual).map(_.as[Expected])
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
                created = actual.as[Created]
              )
            )
          )
        else (otherDestinations, List.empty[Event])
      }
      b3Headers <- Gen.option(b3Gen)
    } yield TestCase(
      origin,
      connectedStations,
      Departure(trainId, destination, expected, actual),
      eventId,
      expectedEvents,
      b3Headers
    )

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def checkDeparture[A](
      departure: Departure,
      httpApp: HttpApp[IO],
      expectedStatus: Status,
      expectedBody: Option[A],
      requestB3Headers: Option[B3Headers]
    )(implicit ev: EntityDecoder[IO, A]) = check(
      httpApp,
      Request(
        method = Method.POST,
        uri = uri"api/v1/departure",
        headers = Headers(`Content-Type`(application.json)) |+| toHeaders(requestB3Headers),
        body = jsonEncoderOf[IO, Departure]
          .toEntity(departure)
          .body
      ),
      requestB3Headers,
      expectedStatus,
      expectedBody
    )

    forall(gen) {
      case TestCase(origin, connectedStations, departure, eventId, expectedEvents, b3Headers) =>
        for {
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          eventSenderStateRef <- EventSenderState.refEmpty
          implicit0(logger: Logger[IO]) <- Slf4jLogger.create[F]
          entryPoint <- TraceEntryPoint.make[IO](TraceProcess("departures-suite"))
          implicit0(uuidGenerator: UUIDGenerator[IO]) = FakeUUIDGenerator.impl[IO](
            uuidGeneratorStateRef
          )
          implicit0(trace: Trace[IO]) = Trace.Implicits.noop[IO]
          httpExpectations <- {
            val httpApp = HttpServer.httpApp(
              FakeArrivals.impl[IO],
              Departures
                .impl[IO](origin, connectedStations, FakeEventSender.impl[IO](eventSenderStateRef)),
              entryPoint
            )
            if (connectedStations.contains_(departure.to))
              checkDeparture(departure, httpApp, Status.Created, eventId.some, b3Headers)
            else
              checkDeparture(
                departure,
                httpApp,
                Status.BadRequest,
                UnexpectedDestination(
                  "Destination is not connected to this station",
                  departure.to
                ).some,
                b3Headers
              )
          }
          sentEvents <- eventSenderStateRef.get
        } yield httpExpectations && expect(sentEvents.events === expectedEvents)
    }
  }
}
