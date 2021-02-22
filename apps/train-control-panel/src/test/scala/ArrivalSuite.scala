package es.eriktorr

import arrival.{Arrival, Arrivals}
import effect._
import spec.HttpRoutesIOSuite
import time.Moment
import time.Moment.When.Actual
import train.TrainId

import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers._
import org.http4s.implicits._

import java.time.Instant

object ArrivalSuite extends HttpRoutesIOSuite {
  override def sharedResource: Resource[IO, Res] =
    TrainControlPanelRoutes.arrivalRoutes[IO](Arrivals.impl[IO]).toResource

  test("create a train arrival") { httpRoutes =>
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    val trainId = TrainId.fromString("C14").toOption.get
    val actual = Moment[Actual](Instant.ofEpochMilli(1614017453487L))
    check(
      httpRoutes = httpRoutes,
      request = Request(
        method = Method.POST,
        uri = uri"arrival",
        headers = Headers.of(`Content-Type`(MediaType.application.json)),
        body = Arrival.arrivalEntityEncoder[IO].toEntity(Arrival(trainId, actual)).body
      ),
      expectedStatus = Status.Created,
      expectedBody = None
    )
  }
}
