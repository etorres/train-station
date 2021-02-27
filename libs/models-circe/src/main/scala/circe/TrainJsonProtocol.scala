package es.eriktorr.train_station
package circe

import train.TrainId

import io.circe._

trait TrainJsonProtocol extends StringFieldDecoder {
  implicit val trainIdDecoder: Decoder[TrainId] = decode[TrainId]("trainId", TrainId.fromString)

  implicit val trainIdEncoder: Encoder[TrainId] = (trainId: TrainId) =>
    Json.obj(("trainId", Json.fromString(trainId.unTrainId.value)))
}
