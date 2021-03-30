package es.eriktorr.train_station
package arrival.infrastructure

import arrival.Arrivals
import arrival.Arrivals.Arrival
import arrival.Arrivals.ArrivalError.UnexpectedTrain
import event.Event.Arrived

import cats.effect.{BracketThrow, Resource}
import cats.implicits._
import cats.{Defer, Monad}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.base.context.Provide
import io.janstenpickle.trace4cats.base.optics.Lens
import io.janstenpickle.trace4cats.model.{SpanKind, SpanStatus}

object ArrivalsTracer {
  def liftTraceContext[F[_]: Monad, G[_]: Defer: BracketThrow, Ctx](
    arrivals: Arrivals[F],
    spanLens: Lens[Ctx, Span[F]]
  )(implicit P: Provide[F, G, Ctx]): Arrivals[G] = (arrival: Arrival) => {
    Resource
      .eval(P.ask[Ctx])
      .flatMap { parentCtx =>
        spanLens
          .get(parentCtx)
          .child(
            arrival.show,
            SpanKind.Internal,
            { case UnexpectedTrain(trainId) =>
              SpanStatus.Internal(show"Unexpected train $trainId")
            }
          )
          .map { childSpan =>
            arrivals.register(arrival).flatTap { _ =>
              childSpan.setStatus(SpanStatus.Ok)
            }
          }
          .mapK(P.liftK)
      }
      .use(P.lift[Arrived])
  }

  def liftTrace[F[_]: Monad, G[_]: Defer: BracketThrow](
    arrivals: Arrivals[F]
  )(implicit P: Provide[F, G, Span[F]]): Arrivals[G] =
    (arrival: Arrival) =>
      P
        .ask[Span[F]]
        .flatMap {
          _.child(
            arrival.show,
            SpanKind.Internal,
            { case UnexpectedTrain(trainId) =>
              SpanStatus.Internal(show"Unexpected train $trainId")
            }
          )
            .map { childSpan =>
              arrivals.register(arrival).flatTap { _ =>
                childSpan.setStatus(SpanStatus.Ok)
              }
            }
            .mapK(P.liftK)
            .use(P.lift)
        }
}
