package es.eriktorr

import scala.util.control.NoStackTrace

object error {
  sealed trait TrainStationError extends NoStackTrace

  object TrainStationError {
    final case class InvalidParameter(error: String) extends TrainStationError
  }
}
