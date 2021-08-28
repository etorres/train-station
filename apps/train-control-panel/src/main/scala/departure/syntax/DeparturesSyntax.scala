package es.eriktorr.train_station
package departure.syntax

import departure.Departures
import departure.Departures.Departure
import departure.Departures.DepartureError.UnexpectedDestination

import cats.implicits._
import cats.{Defer, Monad}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.base.context.Provide
import io.janstenpickle.trace4cats.model.{SpanKind, SpanStatus}
import cats.effect.MonadCancelThrow

trait DeparturesSyntax {
  implicit class DeparturesOps[F[_]: Monad](self: Departures[F]) {
    def liftTrace[G[_]: Defer: MonadCancelThrow](implicit P: Provide[F, G, Span[F]]): Departures[G] =
      (departure: Departure) =>
        P
          .ask[Span[F]]
          .flatMap {
            _.child(
              departure.show,
              SpanKind.Internal,
              { case UnexpectedDestination(_, destination) =>
                SpanStatus.Internal(show"Unexpected destination $destination")
              }
            )
              .map { childSpan =>
                self.register(departure).flatTap { _ =>
                  childSpan.setStatus(SpanStatus.Ok)
                }
              }
              .mapK(P.liftK)
              .use(P.lift)
          }
  }
}
