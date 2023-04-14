package com.avast.micrometer

import cats.effect.{Blocker, ContextShift, Effect, IO}
import com.avast.micrometer.DefaultCatsMeterRegistry.{CollectionSizeToDouble, InitPropertyName}
import com.avast.micrometer.MicrometerJavaConverters.*
import com.avast.micrometer.api.*
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.core.instrument.{
  Counter as _,
  DistributionSummary as JavaSummary,
  Gauge as JavaGauge,
  MeterRegistry as JavaMeterRegistry,
  Tag as _,
  Timer as JavaTimer
}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.DurationConverters.*

private[micrometer] class DefaultCatsMeterRegistry[F[_]: Effect](
    delegate: JavaMeterRegistry,
    blocker: Blocker
) extends CatsMeterRegistry[F] {

  private val logger = LoggerFactory.getLogger(getClass)

  override def underlying: JavaMeterRegistry = delegate

  private val F = Effect[F]

  private implicit val ioCS: ContextShift[IO] = IO.contextShift(blocker.blockingContext)

  private val clock: F[Long] = F.delay(delegate.config().clock().monotonicTime())

  override def counter(name: String, tags: Tag*): Counter[F] = {
    new DefaultCounter(delegate.counter(name, tags.asJavaTags))
  }

  override def gauge[A: ToDouble](name: String, tags: Tag*)(retrieveValue: F[A]): Gauge[F] = {
    val conv: ToDouble[A] = implicitly
    val value: IO[A] = blocker.blockOn(F.toIO(retrieveValue))

    def getValueBlocking: Double = {
      conv.toDouble(value.unsafeRunSync())
    }

    new DefaultGauge(
      JavaGauge
        .builder(name, () => getValueBlocking)
        .tags(tags.asJavaTags)
        .register(delegate)
    )
  }

  override def gaugeCollectionSize[A <: Iterable[?]](name: String, tags: Tag*)(collection: A): Gauge[F] = {
    implicit val toDouble: ToDouble[A] = CollectionSizeToDouble
    gauge(name, tags*)(F.pure(collection))
  }

  override def timer(name: String, tags: Tag*): Timer[F] = {
    new DefaultTimer(
      JavaTimer
        .builder(name)
        .tags(tags.asJavaTags)
        .publishPercentileHistogram()
        .register(delegate),
      clock
    )
  }

  override def timer(name: String, serviceLevelObjectives: Seq[FiniteDuration], tags: Tag*): Timer[F] = {
    new DefaultTimer(
      JavaTimer
        .builder(name)
        .tags(tags.asJavaTags)
        .publishPercentileHistogram()
        .serviceLevelObjectives(serviceLevelObjectives.map(_.toJava)*)
        .register(delegate),
      clock
    )
  }

  override def timer(name: String, minimumExpectedValue: FiniteDuration, maximumExpectedValue: FiniteDuration, tags: Tag*): Timer[F] = {
    new DefaultTimer(
      JavaTimer
        .builder(name)
        .tags(tags.asJavaTags)
        .publishPercentileHistogram()
        .minimumExpectedValue(minimumExpectedValue.toJava)
        .maximumExpectedValue(maximumExpectedValue.toJava)
        .register(delegate),
      clock
    )
  }

  override def timerPair(name: String, tags: Tag*): TimerPair[F] = {
    new DefaultTimerPair(
      timer(name, (Seq(Tag("type", "success")) ++ tags)*),
      timer(name, (Seq(Tag("type", "failure")) ++ tags)*),
      clock
    )
  }

  override def timerPair(
      name: String,
      minimumExpectedValue: FiniteDuration,
      maximumExpectedValue: FiniteDuration,
      tags: Tag*
  ): TimerPair[F] = {
    new DefaultTimerPair(
      timer(name, minimumExpectedValue, maximumExpectedValue, (Seq(Tag("type", "success")) ++ tags)*),
      timer(name, minimumExpectedValue, maximumExpectedValue, (Seq(Tag("type", "failure")) ++ tags)*),
      clock
    )
  }

  override def timerPair(name: String, serviceLevelObjectives: Seq[FiniteDuration], tags: Tag*): TimerPair[F] = {
    new DefaultTimerPair(
      timer(name, serviceLevelObjectives, (Seq(Tag("type", "success")) ++ tags)*),
      timer(name, serviceLevelObjectives, (Seq(Tag("type", "failure")) ++ tags)*),
      clock
    )
  }

  override def summary(name: String, tags: Tag*): DistributionSummary[F] = {
    new DefaultDistributionSummary(delegate.summary(name, tags.asJavaTags))
  }

  override def summary(
      name: String,
      distributionCongfig: DistributionStatisticConfig,
      scale: Double,
      tags: Tag*
  ): DistributionSummary[F] = {
    val conf = distributionCongfig.merge(DistributionStatisticConfig.DEFAULT)

    new DefaultDistributionSummary(
      JavaSummary
        .builder(name)
        .tags(tags.asJavaTags)
        .minimumExpectedValue(conf.getMinimumExpectedValueAsDouble)
        .maximumExpectedValue(conf.getMaximumExpectedValueAsDouble)
        .percentilePrecision(conf.getPercentilePrecision)
        .publishPercentiles(conf.getPercentiles*)
        .publishPercentileHistogram(conf.isPublishingHistogram)
        .distributionStatisticBufferLength(conf.getBufferLength)
        .distributionStatisticExpiry(conf.getExpiry)
        .scale(scale)
        .register(underlying)
    )
  }

  @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  def initIfConfigured(): F[Unit] = {
    if (System.getenv(InitPropertyName) == "true") {
      F.delay {
        logger.warn("Initializing all existing metrics - this may affect your graphs!")

        underlying.getMeters.asScala.foreach {
          _.use(
            _ => (), // gauge is auto-initialized by design
            MetricsInitializer.initCounter,
            MetricsInitializer.initTimer,
            MetricsInitializer.initSummary,
            MetricsInitializer.initLongTaskTimer,
            _ => (), // TimeGauge has no way how to be "initialized" -_-
            _ => (), // FunctionCounter has no way how to be "initialized" -_-
            _ => (), // FunctionTimer has no way how to be "initialized" -_-
            m => throw new IllegalStateException(s"This fallback should have never been invoked! Meter = $m")
          )
        }
      }
    } else F.unit
  }
}

private object DefaultCatsMeterRegistry {

  final val InitPropertyName: String = "CATS_MICROMETER_INIT"

  private object CollectionSizeToDouble extends ToDouble[Iterable[?]] {
    override def toDouble(value: Iterable[?]): Double = value.size.toDouble
  }

}
