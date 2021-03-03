package es.eriktorr.train_station
package effect

import cats.data.NonEmptyList

trait ArraySyntax {
  implicit class ArrayOps[A](self: Array[A]) {
    def toNonEmptyListUnsafe: NonEmptyList[A] = NonEmptyList.fromListUnsafe(self.toList)
  }
}
