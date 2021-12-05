package es.eriktorr.train_station
package json.infrastructure

import io.circe._

trait StringFieldDecoder {
  private[infrastructure] def decode[A](
    field: String,
    fA: String => Either[_ <: Throwable, A]
  ): Decoder[A] =
    (cursor: HCursor) => cursor.downField(field).as[String].flatMap(valueOf(_, fA))

  private[infrastructure] def decodeValue[A](fA: String => Either[_ <: Throwable, A]): Decoder[A] =
    (cursor: HCursor) => cursor.as[String].flatMap(valueOf(_, fA))

  private[this] def valueOf[A](
    str: String,
    fA: String => Either[_ <: Throwable, A]
  ): Decoder.Result[A] =
    fA(str) match {
      case Left(error) => Left(DecodingFailure.fromThrowable(error, List.empty))
      case Right(value) => Right(value)
    }
}
