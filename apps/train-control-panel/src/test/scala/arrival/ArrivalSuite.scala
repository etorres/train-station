package es.eriktorr.train_station
package arrival

import arrival.Arrivals.Arrival
import circe._
import event.EventId
import infrastructure.TrainStationGenerators._
import spec.HttpRoutesIOCheckers
import time.Moment
import time.Moment.When.Actual
import train.TrainId
import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.Show
import cats.derived._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.headers._
import org.http4s.implicits._
import weaver._
import weaver.scalacheck._

object ArrivalSuite extends SimpleIOSuite with Checkers with HttpRoutesIOCheckers {

  test("create train arrivals") {
    final case class TestCase(trainId: TrainId, actual: Moment[Actual], expectedEventId: EventId)

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    val gen = for {
      trainId <- trainIdGen
      actual <- actualGen
      expectedEventId <- eventIdGen
    } yield TestCase(trainId, actual, expectedEventId)

    forall(gen) {
      case TestCase(trainId, actual, expectedEventId) =>
        for {
          uuidGeneratorStateRef <- UUIDGeneratorState.refFrom(expectedEventId.unEventId.value)
          expectations <- {
            implicit val uuidGenerator: UUIDGenerator[IO] =
              FakeUUIDGenerator.impl[IO](uuidGeneratorStateRef)
            check(
              httpRoutes = TrainControlPanelRoutes.arrivalRoutes[IO](Arrivals.impl[IO]),
              request = Request(
                method = Method.POST,
                uri = uri"arrival",
                headers = Headers.of(`Content-Type`(MediaType.application.json)),
                body = Arrival.arrivalEntityEncoder[IO].toEntity(Arrival(trainId, actual)).body
              ),
              expectedStatus = Status.Created,
              expectedBody = expectedEventId.some
            )
          }
        } yield expectations
    }
  }
}
