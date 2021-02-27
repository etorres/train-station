package es.eriktorr.train_station

import scala.util.control.NoStackTrace

object error {
  sealed trait TrainStationError extends NoStackTrace

  object TrainStationError {
    final case class InvalidParameter(error: String) extends TrainStationError {
      override def getMessage: String = error
    }
  }
}
