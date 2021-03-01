package es.eriktorr.train_station
package notification

import event.Event

trait EventSender[F[_]] {
  def send(event: Event): F[Unit]
}
