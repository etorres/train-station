package es.eriktorr.train_station
package arrival

import arrival.Arrivals.Arrival
import arrival.ExpectedTrains.ExpectedTrain
import arrival.infrastructure.FakeExpectedTrains
import arrival.infrastructure.FakeExpectedTrains.ExpectedTrainsState
import effect._
import event.Event.Arrived
import event.{Event, EventId}
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
import time.Moment.When.{Actual, Created}
import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.Show
import cats.data.NonEmptyList
import cats.derived._
import cats.effect._
import cats.implicits._
import io.janstenpickle.trace4cats.inject.Trace
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
      expectedTrain: ExpectedTrain,
      allTrains: NonEmptyList[ExpectedTrain],
      arrival: Arrival,
      eventId: EventId,
      expectedEvents: List[Event]
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
    val gen = for {
      (destination, origins) <- nDistinct(4, stationGen[StationOrigin]).map {
        _.splitAt(1) match {
          case (xs, ys) => (xs.map(_.asStation[Destination]).head, NonEmptyList.fromListUnsafe(ys))
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
                  created = actual.asMoment[Created]
                )
              )
            )
          else (otherExpectedTrains, List.empty[Event])
        }
    } yield TestCase(
      destination,
      expectedTrain,
      allExpectedTrains,
      Arrival(expectedTrain.trainId, actual),
      eventId,
      expectedEvents
    )

    def checkArrival[A](
      arrival: Arrival,
      httpRoutes: HttpRoutes[IO],
      expectedStatus: Status,
      expectedBody: Option[A]
    )(implicit ev: EntityDecoder[IO, A]) = check(
      httpRoutes = httpRoutes,
      request = Request(
        method = Method.POST,
        uri = uri"arrival",
        headers = Headers.of(`Content-Type`(MediaType.application.json)),
        body = Arrival
          .arrivalEntityEncoder[IO]
          .toEntity(arrival)
          .body
      ),
      expectedStatus = expectedStatus,
      expectedBody = expectedBody
    )

    forall(gen) {
      case TestCase(
            destination,
            expectedTrain,
            allExpectedTrains,
            arrival,
            eventId,
            expectedEvents
          ) =>
        for {
          expectedTrainsRef <- ExpectedTrainsState.refFrom(allExpectedTrains.toList)
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(eventId.unEventId.value)
          eventSenderStateRef <- EventSenderState.refEmpty
          implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
          implicit0(uuidGenerator: UUIDGenerator[IO]) = FakeUUIDGenerator.impl[IO](
            uuidGeneratorStateRef
          )
          implicit0(trace: Trace[IO]) = Trace.Implicits.noop[IO]
          httpExpectations <- {
            val httpRoutes = AllHttpRoutes.routes[IO](
              Arrivals.impl[IO](
                destination,
                FakeExpectedTrains.impl[IO](expectedTrainsRef),
                FakeEventSender.impl[IO](eventSenderStateRef)
              )
            )
            if (allExpectedTrains.contains_(expectedTrain))
              checkArrival(arrival, httpRoutes, Status.Created, eventId.some)
            else
              checkArrival(
                arrival,
                httpRoutes,
                Status.BadRequest,
                s"Unexpected train ${expectedTrain.trainId.unTrainId.value}".some
              )
          }
          sentEvents <- eventSenderStateRef.get
        } yield httpExpectations && expect(sentEvents.events === expectedEvents)
    }
  }
}
