package es.eriktorr.train_station
package trace

import cats.effect.{Async, Temporal}
import cats.implicits._
import io.janstenpickle.trace4cats.ToHeaders
import io.janstenpickle.trace4cats.inject.EntryPoint
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.log.LogSpanCompleter
import io.janstenpickle.trace4cats.model.TraceProcess

object TraceEntryPoint {
  def make[F[_]: Async: Temporal](traceProcess: TraceProcess): F[EntryPoint[F]] =
    LogSpanCompleter.create(traceProcess).map { spanCompleter =>
      EntryPoint[F](
        SpanSampler.probabilistic[F](0.05),
        spanCompleter,
        ToHeaders.b3
      )
    }
}
