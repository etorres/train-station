package es.eriktorr.train_station
package avro.infrastructure

import train.TrainId

import cats.implicits._
import vulcan._

trait TrainAvroCodec {
  implicit val trainIdCodec: Codec[TrainId] = Codec.string.imapError(value =>
    TrainId.fromString(value) match {
      case Left(constructorError) => AvroError(constructorError.error).asLeft
      case Right(trainId) => trainId.asRight
    }
  )(_.unTrainId.value)
}
