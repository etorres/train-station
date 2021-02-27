package es.eriktorr.train_station
package circe

import io.circe._

trait StringFieldDecoder {
  private[circe] def decode[A](field: String, fA: String => Either[_ <: Throwable, A]) =
    new Decoder[A] {
      override def apply(cursor: HCursor): Decoder.Result[A] =
        cursor
          .downField(field)
          .as[String]
          .map(fA)
          .fold(
            Left(_), {
              case Left(constructorError) =>
                Left(
                  DecodingFailure(
                    s"Failed to decode: ${cursor.value.toString}, with error: ${constructorError.getMessage}",
                    List.empty
                  )
                )
              case Right(value) => Right(value)
            }
          )
    }
}
