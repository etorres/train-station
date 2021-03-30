package es.eriktorr.train_station

import avro.infrastructure.EventAvroCodec
import event.Event
import jdbc.infrastructure.JdbcTransactor
import messaging.infrastructure.KafkaClient

import cats.effect._
import doobie.Transactor
import fs2.kafka._
import io.janstenpickle.trace4cats.inject.EntryPoint
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.log.LogSpanCompleter
import io.janstenpickle.trace4cats.model.TraceProcess
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext

final case class TrainControlPanelResources[F[_]](
  entryPoint: EntryPoint[F],
  config: TrainControlPanelConfig,
  consumer: KafkaConsumer[F, String, Event],
  producer: KafkaProducer[F, String, Event],
  transactor: Transactor[F]
)

object TrainControlPanelResources extends EventAvroCodec {
  def impl[F[_]: ConcurrentEffect: ContextShift: Timer: Logger](
    executionContext: ExecutionContext,
    blocker: Blocker,
    traceProcess: TraceProcess
  ): Resource[F, TrainControlPanelResources[F]] =
    for {
      entryPoint <- Resource.pure(
        EntryPoint[F](SpanSampler.probabilistic[F](0.05), LogSpanCompleter[F](traceProcess))
      )
      config <- Resource.eval(TrainControlPanelConfig.load[F])
      (consumer, producer) <- KafkaClient.clientsFor(config.kafkaConfig, config.connectedTo)
      transactor <- JdbcTransactor
        .impl[F](config.jdbcConfig, executionContext, blocker)
        .transactorResource
    } yield TrainControlPanelResources(entryPoint, config, consumer, producer, transactor)
}
