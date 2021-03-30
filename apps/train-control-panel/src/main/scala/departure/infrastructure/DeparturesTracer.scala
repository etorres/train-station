package es.eriktorr.train_station
package departure.infrastructure

import departure.Departures
import departure.Departures.Departure
import departure.Departures.DepartureError.UnexpectedDestination
import event.Event.Departed

import cats.effect.{BracketThrow, Resource}
import cats.implicits._
import cats.{Defer, Monad}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.base.context.Provide
import io.janstenpickle.trace4cats.base.optics.Lens
import io.janstenpickle.trace4cats.model.{SpanKind, SpanStatus}

object DeparturesTracer {
  def liftTraceContext[F[_]: Monad, G[_]: Defer: BracketThrow, Ctx](
    departures: Departures[F],
    spanLens: Lens[Ctx, Span[F]]
  )(implicit P: Provide[F, G, Ctx]): Departures[G] = (departure: Departure) => {
    Resource
      .eval(P.ask[Ctx])
      .flatMap { parentCtx =>
        spanLens
          .get(parentCtx)
          .child(
            departure.show,
            SpanKind.Internal,
            { case UnexpectedDestination(destination) =>
              SpanStatus.Internal(show"Unexpected destination $destination")
            }
          )
          .map { childSpan =>
            departures.register(departure).flatTap { _ =>
              childSpan.setStatus(SpanStatus.Ok)
            }
          }
          .mapK(P.liftK)
      }
      .use(P.lift[Departed])
  }

  def liftTrace[F[_]: Monad, G[_]: Defer: BracketThrow](
    departures: Departures[F]
  )(implicit P: Provide[F, G, Span[F]]): Departures[G] =
    (departure: Departure) =>
      P
        .ask[Span[F]]
        .flatMap {
          _.child(
            departure.show,
            SpanKind.Internal,
            { case UnexpectedDestination(destination) =>
              SpanStatus.Internal(show"Unexpected destination $destination")
            }
          )
            .map { childSpan =>
              departures.register(departure).flatTap { _ =>
                childSpan.setStatus(SpanStatus.Ok)
              }
            }
            .mapK(P.liftK)
            .use(P.lift)
        }
}
