package es.eriktorr.train_station
package trace.infrastructure

import cats.data.Kleisli
import cats.effect.{BracketThrow, Sync}
import cats.syntax.applicative._
import cats.syntax.functor._
import io.janstenpickle.trace4cats.base.optics.{Getter, Lens}
import io.janstenpickle.trace4cats.http4s.common.Request_
import io.janstenpickle.trace4cats.inject.Trace
import io.janstenpickle.trace4cats.model.TraceHeaders
import io.janstenpickle.trace4cats.{Span, ToHeaders}
import org.http4s.syntax.string._

import java.util.UUID

final case class TraceContext[F[_]](correlationId: String, span: Span[F])

object TraceContext {
  def traceContextTrace[F[_]: BracketThrow]: Trace[Kleisli[F, TraceContext[F], *]] =
    Trace.kleisliInstance[F].lens[TraceContext[F]](_.span, (c, span) => c.copy(span = span))

  def make[F[_]: Sync](req: Request_, span: Span[F]): F[TraceContext[F]] = {

    // TODO
    println("\n\n >> TraceContext: make\n")
    // TODO

    req.headers
      .get("X-Correlation-ID".ci)
      .fold(Sync[F].delay(UUID.randomUUID().toString))(h => h.value.pure)
      .map(TraceContext(_, span))
  }

  def empty[F[_]: BracketThrow]: F[TraceContext[F]] = {

    // TODO
    println("\n\n >> TraceContext: empty\n")
    // TODO

    Span.noop[F].use(span => TraceContext[F]("", span).pure[F])
  }

  def span[F[_]]: Lens[TraceContext[F], Span[F]] = {

    // TODO
    println("\n\n >> TraceContext: span\n")
    // TODO

    Lens[TraceContext[F], Span[F]](_.span)(s => _.copy(span = s))
  }

  def headers[F[_]](toHeaders: ToHeaders): Getter[TraceContext[F], TraceHeaders] = {

    // TODO
    println("\n\n >> TraceContext: headers\n")
    // TODO

    ctx => toHeaders.fromContext(ctx.span.context) + ("X-Correlation-ID" -> ctx.correlationId)
  }
}
