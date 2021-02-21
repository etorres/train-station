package es.eriktorr
package spec

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.implicits._
import weaver._

trait HttpRoutesIOSuite extends IOSuite {
  override type Res = HttpRoutes[IO]

  def check[A](
    httpRoutes: HttpRoutes[IO],
    request: Request[IO],
    expectedStatus: Status,
    expectedBody: Option[A]
  )(implicit ev: EntityDecoder[IO, A]): IO[Expectations] =
    for {
      response <- httpRoutes.orNotFound.run(request)
      body <- expectedBody.map(_ => response.as[A]).traverse(identity)
    } yield expect(response.status == expectedStatus) && expect(body == expectedBody)
}
