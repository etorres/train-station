package es.eriktorr.train_station
package event_sender

import event.Event

trait EventSender[F[_]] {
  def send(event: Event): F[Unit]
}
