package es.eriktorr

import arrival.Arrivals
import effect._
import spec.HttpRoutesIOSuite

import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

object ArrivalSuite extends HttpRoutesIOSuite {
  override def sharedResource: Resource[IO, Res] =
    TrainControlPanelRoutes.arrivalRoutes[IO](Arrivals.impl[IO]).toResource

  test("create a train arrival") { httpRoutes =>
    check(
      httpRoutes = httpRoutes,
      request = Request(method = Method.POST, uri = uri"arrival"),
      expectedStatus = Status.Created,
      expectedBody = None
    )
  }
}
