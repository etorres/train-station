package es.eriktorr.train_station
package arrival

import arrival.Arrivals.Arrival
import arrival.Arrivals.ArrivalError.UnexpectedTrain
import arrival.ExpectedTrains.ExpectedTrain
import arrival.infrastructure.FakeExpectedTrains
import arrival.infrastructure.FakeExpectedTrains.ExpectedTrainsState
import departure.infrastructure.FakeDepartures
import effect._
import event.Event.Arrived
import event.{Event, EventId}
import http.infrastructure.B3Headers.toHeaders
import http.infrastructure.{B3Headers, HttpServer}
import json.infrastructure.{EventJsonProtocol, TrainJsonProtocol}
import messaging.infrastructure.FakeEventSender
import messaging.infrastructure.FakeEventSender.EventSenderState
import shared.infrastructure.Generators.nDistinct
import shared.infrastructure.TraceGenerators.b3Gen
import shared.infrastructure.TrainControlPanelGenerators.expectedTrainGen
import shared.infrastructure.TrainStationGenerators._
import spec.HttpRoutesIOCheckers
import station.Station
import station.Station.TravelDirection.{Destination, Origin => StationOrigin}
import time.Moment.When.{Actual, Created}
import trace.TraceEntryPoint
import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.Show
import cats.data.NonEmptyList
import cats.derived._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.janstenpickle.trace4cats.inject.Trace
import io.janstenpickle.trace4cats.model.TraceProcess
import org.http4s.MediaType.application
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
    with EventJsonProtocol
    with TrainJsonProtocol {

  test("create train arrivals") {
    final case class TestCase(
      destination: Station[Destination],
      expectedTrain: ExpectedTrain,
      allTrains: NonEmptyList[ExpectedTrain],
      arrival: Arrival,
      eventId: EventId,
      expectedEvents: List[Event],
      b3Headers: Option[B3Headers]
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
    val gen = for {
      (destination, origins) <- nDistinct(4, stationGen[StationOrigin]).map {
        _.splitAt(1) match {
          case (xs, ys) => (xs.map(_.as[Destination]).head, NonEmptyList.fromListUnsafe(ys))
        }
      }
      trainIds <- nDistinct(3, trainIdGen)
      (expectedTrain, otherExpectedTrains) <- trainIds
        .traverse(expectedTrainGen(_, Gen.oneOf(origins.toList)))
        .map {
          _.splitAt(1) match {
            case (xs, ys) => (xs.head, NonEmptyList.fromListUnsafe(ys))
          }
        }
      actual <- momentGen[Actual]
      eventId <- eventIdGen
      (allExpectedTrains, expectedEvents) <- Gen
        .oneOf(true, false)
        .map {
          if (_)
            (
              expectedTrain :: otherExpectedTrains,
              List(
                Arrived(
                  id = eventId,
                  trainId = expectedTrain.trainId,
                  from = expectedTrain.from,
                  to = destination,
                  expected = expectedTrain.expected,
                  created = actual.as[Created]
                )
              )
            )
          else (otherExpectedTrains, List.empty[Event])
        }
      b3Headers <- Gen.option(b3Gen)
    } yield TestCase(
      destination,
      expectedTrain,
      allExpectedTrains,
      Arrival(expectedTrain.trainId, actual),
      eventId,
      expectedEvents,
      b3Headers
    )

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def checkArrival[A](
      arrival: Arrival,
      httpApp: HttpApp[IO],
      expectedStatus: Status,
      expectedBody: Option[A],
      requestB3Headers: Option[B3Headers]
    )(implicit ev: EntityDecoder[IO, A]) = check(
      httpApp,
      Request(
        method = Method.POST,
        uri = uri"api/v1/arrival",
        headers = Headers(`Content-Type`(application.json)) |+| toHeaders(requestB3Headers),
        body = Arrival
          .arrivalEntityEncoder[IO]
          .toEntity(arrival)
          .body
      ),
      requestB3Headers,
      expectedStatus,
      expectedBody
    )

    implicit val unexpectedTrainDecoder: Decoder[UnexpectedTrain] = deriveDecoder

    forall(gen) {
      case TestCase(
            destination,
            expectedTrain,
            allExpectedTrains,
            arrival,
            eventId,
            expectedEvents,
            b3Headers
          ) =>
        for {
          expectedTrainsRef <- ExpectedTrainsState.refFrom(allExpectedTrains.toList)
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          eventSenderStateRef <- EventSenderState.refEmpty
          entryPoint <- TraceEntryPoint.make[IO](TraceProcess("arrivals-suite"))
          implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
          implicit0(uuidGenerator: UUIDGenerator[IO]) = FakeUUIDGenerator.impl[IO](
            uuidGeneratorStateRef
          )
          implicit0(trace: Trace[IO]) = Trace.Implicits.noop[IO]
          httpExpectations <- {
            val httpApp = HttpServer.httpApp(
              Arrivals
                .impl[IO](
                  destination,
                  FakeExpectedTrains.impl[IO](expectedTrainsRef),
                  FakeEventSender.impl[IO](eventSenderStateRef)
                ),
              FakeDepartures.impl[IO],
              entryPoint
            )
            if (allExpectedTrains.contains_(expectedTrain))
              checkArrival(arrival, httpApp, Status.Created, eventId.some, b3Headers)
            else
              checkArrival(
                arrival,
                httpApp,
                Status.BadRequest,
                UnexpectedTrain(
                  "There is no recorded departure for the train",
                  expectedTrain.trainId
                ).some,
                b3Headers
              )
          }
          sentEvents <- eventSenderStateRef.get
        } yield httpExpectations && expect(sentEvents.events === expectedEvents)
    }
  }
}
