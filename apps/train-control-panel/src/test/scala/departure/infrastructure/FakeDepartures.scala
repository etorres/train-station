package es.eriktorr.train_station
package departure.infrastructure

import departure.Departures
import departure.Departures.Departure
import event.Event.Departed

import cats.effect.Sync

final class FakeDepartures[F[_]: Sync] extends Departures[F] {
  override def register(departure: Departure): F[Departed] =
    F.raiseError(new IllegalStateException("not implemented"))
}

object FakeDepartures {
  def impl[F[_]: Sync] = new FakeDepartures[F]
}
