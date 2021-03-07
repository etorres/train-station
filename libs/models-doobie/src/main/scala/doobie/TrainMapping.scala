package es.eriktorr.train_station
package doobie

import train.TrainId

import _root_.doobie._

trait TrainMapping {
  implicit val trainIdPut: Put[TrainId] = Put[String].contramap(_.unTrainId.value)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit val trainIdRead: Read[TrainId] = Read[String].map(TrainId.fromString(_).toOption.get)
}
