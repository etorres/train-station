package es.eriktorr.train_station
package jdbc.infrastructure

import train.TrainId

import doobie._

trait TrainMapping {
  implicit val trainIdPut: Put[TrainId] = Put[String].contramap(_.unTrainId.value)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit val trainIdRead: Read[TrainId] = Read[String].map(TrainId.fromString(_).toOption.get)
}
