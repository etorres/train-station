package es.eriktorr.train_station
package jdbc.infrastructure

import TrainControlPanelConfig.JdbcConfig

import cats.effect._
import _root_.doobie.hikari._

import scala.concurrent.ExecutionContext

final class JdbcTransactor[F[_]: Async] private[infrastructure] (jdbcConfig: JdbcConfig)(
  implicit connectEc: ExecutionContext,
  blocker: Blocker,
  contextShift: ContextShift[F]
) {
  val transactorResource: Resource[F, HikariTransactor[F]] =
    for {
      xa <- HikariTransactor.newHikariTransactor[F](
        jdbcConfig.driverClassName.value,
        jdbcConfig.connectUrl.value,
        jdbcConfig.user.value,
        jdbcConfig.password.value.value,
        connectEc,
        blocker
      )
    } yield xa
}

object JdbcTransactor {
  def impl[F[_]: Async](jdbcConfig: JdbcConfig)(
    implicit connectEc: ExecutionContext,
    blocker: Blocker,
    contextShift: ContextShift[F]
  ): JdbcTransactor[F] = new JdbcTransactor(jdbcConfig)
}
