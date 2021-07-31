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
            response.headers.get[`X-B3-TraceId`].map(_.asUUID.show.replace("-", "")),
            response.headers.get[`X-B3-Sampled`].map(h => if (h.sampled) "1" else "0")
          )
        case None => (none[String], none[String])
      }
    } yield expect(response.status == expectedStatus) && expect(body == expectedBody) && expect(
      requestB3Headers.map(_.traceId.show) === traceIdResponseHeader
    ) && expect(requestB3Headers.map(_.sampled.show) === sampledResponseHeader)
}
