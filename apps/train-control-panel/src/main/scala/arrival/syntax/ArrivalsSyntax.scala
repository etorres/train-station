package es.eriktorr.train_station
package arrival.syntax

import arrival.Arrivals
import arrival.Arrivals.Arrival
import arrival.Arrivals.ArrivalError.UnexpectedTrain

import cats.Defer
import cats.effect.MonadCancelThrow
import cats.implicits._
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.base.context.Provide
import io.janstenpickle.trace4cats.model.{SpanKind, SpanStatus}

trait ArrivalsSyntax {
  implicit class ArrivalsOps[F[_]: MonadCancelThrow](self: Arrivals[F]) {
    def liftTrace[G[_]: Defer: MonadCancelThrow](implicit P: Provide[F, G, Span[F]]): Arrivals[G] =
      (arrival: Arrival) =>
        P
          .ask[Span[F]]
          .flatMap {
            _.child(
              arrival.show,
              SpanKind.Internal,
              { case UnexpectedTrain(_, trainId) =>
                SpanStatus.Internal(show"Unexpected train $trainId")
              }
            )
              .map { childSpan =>
                self.register(arrival).flatTap { _ =>
                  childSpan.setStatus(SpanStatus.Ok)
                }
              }
              .mapK(P.liftK)
              .use(P.lift)
          }
  }
}
