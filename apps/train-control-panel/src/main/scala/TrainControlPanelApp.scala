package es.eriktorr.train_station

import arrival.Arrivals
import arrival.infrastructure.JdbcExpectedTrains
import departure.{DepartureTracker, Departures}
import http.infrastructure.HttpServer
import messaging.infrastructure.{KafkaDepartureListener, KafkaEventSender}
import station.Station.TravelDirection.Destination
import trace.TraceEntryPoint

import cats.NonEmptyParallel
import cats.effect._
import cats.implicits._
import io.janstenpickle.trace4cats.inject.{EntryPoint, Trace}
import io.janstenpickle.trace4cats.model.TraceProcess
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import cats.effect.Temporal

object TrainControlPanelApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    def program[F[_]: ConcurrentEffect: ContextShift: Temporal: NonEmptyParallel: Logger: Trace](
      executionContext: ExecutionContext,
      entryPoint: EntryPoint[F]
    ): F[Unit] =
      TrainControlPanelResources.impl[F](executionContext, blocker).use {
        case TrainControlPanelResources(config, consumer, producer, transactor) =>
          val expectedTrains = JdbcExpectedTrains.impl[F](transactor)
          val departureTracker =
            DepartureTracker.impl[F](config.station.as[Destination], expectedTrains)

          val departureListener =
            KafkaDepartureListener.stream[F](consumer, departureTracker).compile.drain

          val eventSender = KafkaEventSender.impl[F](producer, config.kafkaConfig.topic)
          val arrivals =
            Arrivals.impl[F](config.station.as[Destination], expectedTrains, eventSender)
          val departures = Departures.impl[F](config.station, config.connectedTo, eventSender)

          val httpServer = HttpServer
            .stream[F](
              arrivals,
              departures,
              entryPoint,
              config.httpServerConfig,
              executionContext
            )
            .compile
            .drain

          F.info(show"Started train station ${config.station}") *> (
            departureListener,
            httpServer
          ).parMapN((_, _) => ())
      }

    (for {
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
      implicit0(trace: Trace[IO]) = Trace.Implicits.noop[IO]
      entryPoint <- TraceEntryPoint.make[IO](TraceProcess("train-control-panel"))
      result <- program[IO](
        executionContext,
        Blocker.liftExecutionContext(executionContext),
        entryPoint
      )
    } yield result).as(ExitCode.Success)
  }
}
