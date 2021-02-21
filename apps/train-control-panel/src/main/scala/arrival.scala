package es.eriktorr

import event.Event.Arrived
import time.Moment
import time.Moment.When.Actual
import train.TrainId

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import org.http4s._
import org.http4s.circe._

object arrival {
  final case class Arrival(trainId: TrainId, actual: Moment[Actual])

  object Arrival {
    implicit val trainIdDecoder: Decoder[TrainId] = (c: HCursor) =>
      Left(DecodingFailure(s"Can't decode: ${c.value.toString}", List()))
    implicit def momentDecoder[A <: Moment.When]: Decoder[Moment[A]] =
      (c: HCursor) => Left(DecodingFailure(s"Can't decode: ${c.value.toString}", List()))

    implicit val trainIdEncoder: Encoder[TrainId] = ???
    implicit def momentEncoder[A <: Moment.When]: Encoder[Moment[A]] = ???

    implicit val arrivalDecoder: Decoder[Arrival] = deriveDecoder
    implicit def arrivalEntityDecoder[F[_]: Sync]: EntityDecoder[F, Arrival] = jsonOf

    implicit val arrivalEncoder: Encoder[Arrival] = deriveEncoder
    implicit def arrivalEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Arrival] = jsonEncoderOf
  }

  sealed trait ArrivalError

  object ArrivalError {
    final case class UnexpectedTrain(trainId: TrainId) extends ArrivalError
  }

  trait Arrivals[F[_]] {
    def register(arrival: Arrival): F[Either[ArrivalError, Arrived]]
  }

  object Arrivals {
    def impl[F[_]: Sync]: Arrivals[F] = new Arrivals[F] {
      override def register(arrival: Arrival): F[Either[ArrivalError, Arrived]] =
        for {
          trainId <- F.fromEither(TrainId.fromString("123"))
        } yield Left(ArrivalError.UnexpectedTrain(trainId))
    }
  }
}
