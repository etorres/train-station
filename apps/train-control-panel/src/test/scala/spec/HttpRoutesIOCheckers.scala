package es.eriktorr.train_station
package spec

import http.infrastructure.B3Headers

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers.{`X-B3-Sampled`, `X-B3-TraceId`}
import weaver.Expectations
import weaver.Expectations.Helpers.expect

trait HttpRoutesIOCheckers {
  def check[A](
    httpApp: HttpApp[IO],
    request: Request[IO],
    requestB3Headers: Option[B3Headers] = none[B3Headers],
    expectedStatus: Status,
    expectedBody: Option[A]
  )(implicit ev: EntityDecoder[IO, A]): IO[Expectations] =
    for {
      response <- httpApp.run(request)
      body <- expectedBody.map(_ => response.as[A]).traverse(identity)
      (traceIdResponseHeader, sampledResponseHeader) = requestB3Headers match {
        case Some(_) =>
          (
            response.headers.find(_.is(`X-B3-TraceId`)).map(_.value),
            response.headers.find(_.is(`X-B3-Sampled`)).map(_.value)
          )
        case None => (none[Header], none[Header])
      }
    } yield expect(response.status == expectedStatus) && expect(body == expectedBody) && expect(
      requestB3Headers.map(_.traceId.show) == traceIdResponseHeader
    ) && expect(
      requestB3Headers.map(_.sampled.show) == sampledResponseHeader
    )
}
