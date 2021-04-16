package com.avast.micrometer

import cats.effect.{Blocker, Effect}
import com.avast.micrometer.MicrometerJavaConverters._
import com.avast.micrometer.api.Tag
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.simple.{SimpleConfig, SimpleMeterRegistry}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class TestCatsMeterRegistry[F[_]: Effect](clock: Clock = Clock.SYSTEM)
    extends DefaultCatsMeterRegistry[F](
      new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock),
      Blocker.liftExecutionContext(ExecutionContext.global)
    ) {

  def getCounterCount(name: String, tags: Tag*): Double = {
    underlying.counter(name, tags.asJavaTags).count()
  }

  def getTimerStats(name: String, tags: Tag*): TimerStats = {
    val timer = underlying.timer(name, tags.asJavaTags)
    TimerStats(timer.count(), timer.totalTime(TimeUnit.NANOSECONDS).nanos)
  }

  def getTimerPairStats(name: String, tags: Tag*): TimerPairStats = {
    val succ = underlying.timer(name, (Seq(Tag("type", "success")) ++ tags).asJavaTags)
    val fail = underlying.timer(name, (Seq(Tag("type", "failure")) ++ tags).asJavaTags)

    TimerPairStats(
      successes = TimerStats(succ.count(), succ.totalTime(TimeUnit.NANOSECONDS).nanos),
      failures = TimerStats(fail.count(), fail.totalTime(TimeUnit.NANOSECONDS).nanos)
    )
  }

  def getSummaryStats(name: String, tags: Tag*): SummaryStats = {
    val summ = underlying.summary(name, tags.asJavaTags)

    SummaryStats(
      count = summ.count(),
      total = summ.totalAmount(),
      max = summ.max()
    )
  }

}

final case class TimerStats(count: Long, totalTime: Duration)

final case class TimerPairStats(successes: TimerStats, failures: TimerStats)

final case class SummaryStats(count: Long, total: Double, max: Double)
