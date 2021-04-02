package es.eriktorr.train_station
package arrival.infrastructure

import arrival.Arrivals
import arrival.Arrivals.Arrival
import event.Event.Arrived

import cats.effect.Sync

final class FakeArrivals[F[_]: Sync] extends Arrivals[F] {
  override def register(arrival: Arrival): F[Arrived] =
    F.raiseError(new IllegalStateException("not implemented"))
}

object FakeArrivals {
  def impl[F[_]: Sync] = new FakeArrivals[F]
}
