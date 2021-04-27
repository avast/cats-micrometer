package com.avast.micrometer

import cats.effect.{Blocker, ContextShift, Effect, IO}
import com.avast.micrometer.DefaultCatsMeterRegistry.{CollectionSizeToDouble, InitPropertyName}
import com.avast.micrometer.MicrometerJavaConverters._
import com.avast.micrometer.api._
import io.micrometer.core.instrument.{Counter => _, Gauge => JavaGauge, MeterRegistry => JavaMeterRegistry, Tag => _, Timer => _}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.CollectionHasAsScala

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

  override def gaugeCollectionSize[A <: Iterable[_]](name: String, tags: Tag*)(collection: A): Gauge[F] = {
    gauge(name, tags: _*)(F.pure(collection))(CollectionSizeToDouble)
  }

  override def timer(name: String, tags: Tag*): Timer[F] = {
    new DefaultTimer(delegate.timer(name, tags.asJavaTags), clock)
  }

  override def timerPair(name: String, tags: Tag*): TimerPair[F] = {
    new DefaultTimerPair(
      timer(name, Seq(Tag("type", "success")) ++ tags: _*),
      timer(name, Seq(Tag("type", "failure")) ++ tags: _*),
      clock
    )
  }

  override def summary(name: String, tags: Tag*): DistributionSummary[F] = {
    new DefaultDistributionSummary(delegate.summary(name, tags.asJavaTags))
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

  private object CollectionSizeToDouble extends ToDouble[Iterable[_]] {
    override def toDouble(value: Iterable[_]): Double = value.size.toDouble
  }

}
