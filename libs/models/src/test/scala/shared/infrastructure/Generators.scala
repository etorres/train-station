package es.eriktorr.train_station
package shared.infrastructure

import cats.data.NonEmptyList
import org.scalacheck.Gen

object Generators {
  def nDistinct[T](number: Int, elementGen: Gen[T]): Gen[NonEmptyList[T]] = {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def generate(accumulator: List[T]): Gen[List[T]] =
      if (accumulator.size == number) Gen.const(accumulator)
      else
        for {
          candidate <- elementGen
          result <- generate(
            if (accumulator.contains(candidate)) accumulator else candidate :: accumulator
          )
        } yield result

    generate(List.empty).map(NonEmptyList.fromListUnsafe)
  }
}
