package es.eriktorr.train_station
package spec

import cats.effect._
import cats.implicits._
import org.http4s._
import weaver.Expectations
import weaver.Expectations.Helpers.expect

trait HttpRoutesIOCheckers {
  def check[A](
    httpApp: HttpApp[IO],
    request: Request[IO],
    expectedStatus: Status,
    expectedBody: Option[A]
  )(implicit ev: EntityDecoder[IO, A]): IO[Expectations] =
    for {
      response <- httpApp.run(request)
      body <- expectedBody.map(_ => response.as[A]).traverse(identity)
    } yield expect(response.status == expectedStatus) && expect(body == expectedBody)
}
