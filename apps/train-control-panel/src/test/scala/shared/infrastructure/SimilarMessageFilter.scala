package es.eriktorr.train_station
package shared.infrastructure

import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.spi.FilterReply
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.slf4j.Marker

import scala.beans.BeanProperty
import scala.concurrent.duration._

/**
 * Inefficient Logback filter to remove similar logs based on simple duplication rules.
 *
 * Calls to `decide` method (overridden from [[ch.qos.logback.classic.turbo.TurboFilter]]) could be synchronized.
 *
 * @example {{{
 * <turboFilter class="es.eriktorr.train_station.shared.infrastructure.SimilarMessageFilter">
 *   <includedLoggers>package.ClassA,package.ClassB</includedLoggers>
 *   <prefixes>prefix A,prefix B</prefixes>
 * </turboFilter>
 * }}}
 *
 * @see See [[http://logback.qos.ch/manual/filters.html Logback Filters]].
 */
final class SimilarMessageFilter extends TurboFilter {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @BeanProperty var prefixes: String = ""

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @BeanProperty var includedLoggers: String = ""

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @BeanProperty var allowedRepetitions = 1

  override def decide(
    marker: Marker,
    logger: Logger,
    level: Level,
    format: String,
    params: Array[AnyRef],
    t: Throwable
  ): FilterReply =
    if (isStarted && format != null && includedLoggers.split(",").contains(logger.getName)) {
      prefixes.split(",").find(format.startsWith) match {
        case Some(prefix) =>
          this.synchronized {
            SimilarMessageFilter.cache.getIfPresent(prefix) match {
              case Some(count) =>
                SimilarMessageFilter.cache.put(prefix, count + 1)
                if (count < allowedRepetitions) FilterReply.NEUTRAL else FilterReply.DENY
              case None =>
                SimilarMessageFilter.cache.put(prefix, 1)
                FilterReply.NEUTRAL
            }
          }
        case None => FilterReply.NEUTRAL
      }
    } else FilterReply.NEUTRAL
}

object SimilarMessageFilter {
  private[infrastructure] val cache: Cache[String, Int] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(30.seconds)
      .initialCapacity(100)
      .maximumSize(100)
      .build[String, Int]()
}
