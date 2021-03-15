package es.eriktorr.train_station
package effect

import cats.data.NonEmptyList

trait NonEmptyListSyntax {
  implicit class NonEmptyListOps[A](self: NonEmptyList[A]) {
    def splitAt(n: Int): (List[A], List[A]) = self.toList.splitAt(n)
  }
}
