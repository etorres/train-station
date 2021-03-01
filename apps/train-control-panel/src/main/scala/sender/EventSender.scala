package es.eriktorr.train_station
package sender

import event.Event

trait EventSender[F[_]] {
  def send(event: Event): F[Unit]
}
