package es.eriktorr.train_station
package jdbc.infrastructure

import TrainControlPanelConfig.JdbcConfig

import cats.effect._
import doobie.hikari._

import scala.concurrent.ExecutionContext

final class JdbcTransactor[F[_]: Async: ContextShift] private[infrastructure] (
  jdbcConfig: JdbcConfig,
  connectEc: ExecutionContext,
  blocker: Blocker
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
  def impl[F[_]: Async: ContextShift](
    jdbcConfig: JdbcConfig,
    connectEc: ExecutionContext): JdbcTransactor[F] =
    new JdbcTransactor(jdbcConfig, connectEc, blocker)
}
