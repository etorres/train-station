package es.eriktorr.train_station
package trace

import cats.effect.{ConcurrentEffect, Timer}
import io.janstenpickle.trace4cats.ToHeaders
import io.janstenpickle.trace4cats.inject.EntryPoint
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.log.LogSpanCompleter
import io.janstenpickle.trace4cats.model.TraceProcess
import org.typelevel.log4cats.Logger

object TraceEntryPoint {
  def impl[F[_]: ConcurrentEffect: Timer: Logger](traceProcess: TraceProcess): EntryPoint[F] =
    EntryPoint[F](
      SpanSampler.probabilistic[F](0.05),
      LogSpanCompleter[F](traceProcess),
      ToHeaders.b3
    )
}
