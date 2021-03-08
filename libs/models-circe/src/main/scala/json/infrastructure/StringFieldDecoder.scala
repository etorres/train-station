package es.eriktorr.train_station
package json.infrastructure

import io.circe._

trait StringFieldDecoder {
  private[infrastructure] def decode[A](
    field: String,
    fA: String => Either[_ <: Throwable, A]
  ): Decoder[A] =
    (cursor: HCursor) =>
      cursor
        .downField(field)
        .as[String]
        .map(fA)
        .fold(Left(_), valueOf(cursor.value))

  private[infrastructure] def decodeValue[A](fA: String => Either[_ <: Throwable, A]): Decoder[A] =
    (cursor: HCursor) =>
      cursor
        .as[String]
        .map(fA)
        .fold(Left(_), valueOf(cursor.value))

  private[this] def valueOf[A](
    json: Json
  ): Either[_ <: Throwable, A] => Either[DecodingFailure, A] = {
    case Left(constructorError) =>
      Left(
        DecodingFailure(
          s"Failed to decode: ${json.toString}, with error: ${constructorError.getMessage}",
          List.empty
        )
      )
    case Right(value) => Right(value)
  }
}
