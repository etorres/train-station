package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals
import arrival.Arrivals.ArrivalError.UnexpectedTrain
import arrival.Arrivals.{Arrival, ArrivalError}
import departure.Departures
import departure.Departures.DepartureError.UnexpectedDestination
import departure.Departures.{Departure, DepartureError}
import event.EventId
import json.infrastructure.{EventJsonProtocol, StationJsonProtocol, TrainJsonProtocol}
import station.Station
import station.Station.TravelDirection.Destination
import time.Moment
import time.Moment.When.{Actual, Expected}
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
import sttp.tapir._
import sttp.tapir.codec.newtype._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.time.OffsetDateTime

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object OpenApiEndpoints extends EventJsonProtocol with StationJsonProtocol with TrainJsonProtocol {

  type StreamTypes[F[_]] = Fs2Streams[F] with WebSockets

  implicit val arrivalErrorEncoder: Encoder[ArrivalError] = Encoder.instance {
    case unexpectedTrain @ UnexpectedTrain(_, _) => unexpectedTrain.asJson
  }

  implicit val departureErrorEncoder: Encoder[DepartureError] = Encoder.instance {
    case unexpectedDestination @ UnexpectedDestination(_, _) => unexpectedDestination.asJson
  }

  def arrivalEndpoint[F[_]: Sync]: Endpoint[Arrival, ArrivalError, EventId, StreamTypes[F]] = {
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

  def departureEndpoint[F[_]: Sync]
    : Endpoint[Departure, DepartureError, EventId, StreamTypes[F]] = {
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    val departureBody = jsonBody[Departure]
      .description("The departure")
      .example(
        Departure(
          TrainId.fromString("2b424db4-b111-4f27-8c7c-70f866c3cc50").toOption.get,
          Station.fromString[Destination]("Barcelona").toOption.get,
          Moment[Expected](OffsetDateTime.parse("2021-03-08T22:05:42Z")),
          Moment[Actual](OffsetDateTime.parse("2021-03-08T18:05:42Z"))
        )
      )

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    val eventIdResponse = jsonBody[EventId]
      .description("An event Id")
      .example(EventId.fromString("49dbf1d9-d982-4f2e-8065-bd5a79078987").toOption.get)

    endpoint
      .description("Register a departure, if the destination is reachable")
      .post
      .in("api" / "v1" / "departure")
      .in(departureBody)
      .out(statusCode(StatusCode.Created).and(eventIdResponse))
      .errorOut(jsonBody[DepartureError])
  }

  def routes[F[_]: Sync: Concurrent: ContextShift: Timer](
    A: Arrivals[F],
    D: Departures[F]
  ): HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRouteRecoverErrors(arrivalEndpoint)(
      A.register(_).map(_.id)
    ) <+> Http4sServerInterpreter[F]().toRouteRecoverErrors(departureEndpoint)(
      D.register(_).map(_.id)
    )

  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.circe.yaml._
  import sttp.tapir.swagger.http4s.SwaggerHttp4s

  def swaggerRoute[F[_]: Sync: ContextShift]: HttpRoutes[F] = new SwaggerHttp4s(
    OpenAPIDocsInterpreter()
      .toOpenAPI(List(arrivalEndpoint, departureEndpoint), "Train Control Panel", "v1")
      .toYaml
  ).routes
}
