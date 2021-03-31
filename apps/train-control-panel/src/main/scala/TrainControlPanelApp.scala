package es.eriktorr.train_station

import arrival.Arrivals
import arrival.infrastructure.JdbcExpectedTrains
import departure.{DepartureTracker, Departures}
import http.infrastructure.HttpServer
import messaging.infrastructure.{KafkaDepartureListener, KafkaEventSender}
import station.Station.TravelDirection.Destination

import cats.NonEmptyParallel
import cats.effect._
import cats.implicits._
import io.janstenpickle.trace4cats.ToHeaders
import io.janstenpickle.trace4cats.inject.{EntryPoint, Trace}
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.log.LogSpanCompleter
import io.janstenpickle.trace4cats.model.TraceProcess
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object TrainControlPanelApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    def program[F[_]: ConcurrentEffect: ContextShift: Timer: NonEmptyParallel: Logger: Trace](
      executionContext: ExecutionContext,
      blocker: Blocker,
      traceProcess: TraceProcess
    ): F[Unit] =
      TrainControlPanelResources.impl[F](executionContext, blocker, traceProcess).use {
        case TrainControlPanelResources(config, consumer, producer, transactor) =>
          val expectedTrains = JdbcExpectedTrains.impl[F](transactor)
          val departureTracker =
            DepartureTracker.impl[F](config.station.asStation[Destination], expectedTrains)

          val departureListener =
            KafkaDepartureListener.stream[F](consumer, departureTracker).compile.drain

          val eventSender = KafkaEventSender.impl[F](producer, config.kafkaConfig.topic)
          val arrivals =
            Arrivals.impl[F](config.station.asStation[Destination], expectedTrains, eventSender)
          val departures = Departures.impl[F](config.station, config.connectedTo, eventSender)

          val entryPoint =
            EntryPoint[F](
              SpanSampler.probabilistic[F](0.05),
              LogSpanCompleter[F](traceProcess),
              ToHeaders.b3
            )

          val httpServer = HttpServer
            .stream[F](
              arrivals,
              departures,
              config.httpServerConfig,
              executionContext,
              entryPoint
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
      result <- program[IO](
        executionContext,
        Blocker.liftExecutionContext(executionContext),
        TraceProcess("train-control-panel")
      )
    } yield result).as(ExitCode.Success)
  }
}
