package es.eriktorr.train_station
package http.infrastructure

import arrival.Arrivals
import arrival.Arrivals.ArrivalError.UnexpectedTrain
import arrival.Arrivals.{Arrival, ArrivalError}
import departure.Departures
import departure.Departures.{Departure, DepartureError}
import event.EventId
import json.infrastructure.{EventJsonProtocol, StationJsonProtocol, TrainJsonProtocol}
import station.Station
import station.Station.TravelDirection.Destination
import time.Moment
import time.Moment.When.{Actual, Expected}
import train.TrainId

import cats.effect.{Async, Sync}
import cats.implicits._
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.HttpRoutes
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.newtype._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.time.OffsetDateTime

object OpenApiEndpoints extends EventJsonProtocol with StationJsonProtocol with TrainJsonProtocol {

  implicit val arrivalErrorEncoder: Encoder[ArrivalError] = Encoder.instance {
    case unexpectedTrain @ UnexpectedTrain(_, _) => unexpectedTrain.asJson
  }

  def arrivalEndpoint[F[_]: Sync]: Endpoint[Unit, Arrival, ArrivalError, EventId, Fs2Streams[F]] = {
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
    : Endpoint[Unit, Departure, DepartureError, EventId, Fs2Streams[F]] = {
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

  def routes[F[_]: Async](A: Arrivals[F], D: Departures[F]): HttpRoutes[F] = {
    @SuppressWarnings(
      Array(
        "org.wartremover.warts.JavaSerializable",
        "org.wartremover.warts.Product",
        "org.wartremover.warts.Serializable"
      )
    )
    val serverEndpoints = List(
      arrivalEndpoint.serverLogicRecoverErrors(A.register(_).map(_.id)),
      departureEndpoint.serverLogicRecoverErrors(D.register(_).map(_.id))
    )
    Http4sServerInterpreter().toRoutes(serverEndpoints)
  }

  // TODO
  def swaggerRoute2[F[_]: Async]: HttpRoutes[F] = {
    val swaggerEndpoints = SwaggerInterpreter()
      .fromEndpoints[F](List(arrivalEndpoint, departureEndpoint), "Train Control Panel", "v1")
    Http4sServerInterpreter().toRoutes(swaggerEndpoints)
  }

  def swaggerRoute[F[_]: Async]: HttpRoutes[F] = {
    val openAPIYaml = OpenAPIDocsInterpreter()
      .toOpenAPI(List(arrivalEndpoint, departureEndpoint), "Train Control Panel", "v1")
      .toYaml
    Http4sServerInterpreter().toRoutes(SwaggerUI[F](openAPIYaml))
  }
}
