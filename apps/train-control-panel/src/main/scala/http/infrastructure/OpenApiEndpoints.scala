package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals
import arrival.Arrivals.ArrivalError.UnexpectedTrain
import arrival.Arrivals.{Arrival, ArrivalError}
import event.EventId
import json.infrastructure.{EventJsonProtocol, TrainJsonProtocol}
import time.Moment
import time.Moment.When.Actual
import train.TrainId

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.StatusCode
import sttp.tapir.CodecFormat.Json
import sttp.tapir._
import sttp.tapir.codec.newtype._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.time.OffsetDateTime

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object OpenApiEndpoints extends EventJsonProtocol with TrainJsonProtocol {
  implicit val eventIdCodec: Codec[String, EventId, Json] = Codec.json[EventId] { str =>
    EventId.fromString(str) match {
      case Left(error) => DecodeResult.Error(str, error)
      case Right(value) => DecodeResult.Value(value)
    }
  }(_.unEventId.value)
  implicitly[Schema[EventId]]

  implicit val arrivalErrorEncoder: Encoder[ArrivalError] = Encoder.instance {
    case unexpectedTrain @ UnexpectedTrain(_) => unexpectedTrain.asJson
  }

  def arrivalEndpoint[F[_]: Sync]: Endpoint[Arrival, ArrivalError, EventId, Fs2Streams[
    F
  ] with WebSockets] = {
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    val arrivalBody = jsonBody[Arrival]
      .description("The arrival")
      .example(
        Arrival(
          TrainId.fromString("2b424db4-b111-4f27-8c7c-70f866c3cc50").toOption.get,
          Moment[Actual](OffsetDateTime.parse("2021-03-08T22:05:42Z"))
        )
      )

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    val eventIdResponse = jsonBody[EventId]
      .description("An event Id")
      .example(EventId.fromString("49dbf1d9-d982-4f2e-8065-bd5a79078987").toOption.get)

    endpoint
      .description("Register an arrival, if the train is expected")
      .post
      .in("api" / "v1" / "arrival")
      .in(arrivalBody)
      .out(statusCode(StatusCode.Created).and(eventIdResponse))
      .errorOut(jsonBody[ArrivalError])
  }

  def routes[F[_]: Sync](
    A: Arrivals[F]
  )(implicit fs: Concurrent[F], fcs: ContextShift[F], timer: Timer[F]): HttpRoutes[F] =
    Http4sServerInterpreter.toRouteRecoverErrors(arrivalEndpoint)(A.register(_).map(_.id))

  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.circe.yaml._
  import sttp.tapir.swagger.http4s.SwaggerHttp4s

  def docs[F[_]: Sync]: OpenAPI =
    OpenAPIDocsInterpreter.toOpenAPI(List(arrivalEndpoint), "Train Control Panel", "v1")

  def docsAsYaml[F[_]: Sync]: String = docs.toYaml

  def swaggerRoute[F[_]: Sync: ContextShift]: HttpRoutes[F] = new SwaggerHttp4s(
    docsAsYaml
  ).routes
}
