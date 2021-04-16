package com.avast.micrometer

import cats.effect.{Blocker, ContextShift, Effect, IO}
import com.avast.micrometer.DefaultCatsMeterRegistry.CollectionSizeToDouble
import com.avast.micrometer.MicrometerJavaConverters._
import com.avast.micrometer.api._
import io.micrometer.core.instrument.{Counter => _, Gauge => JavaGauge, MeterRegistry => JavaMeterRegistry, Tag => _, Timer => _}

private[micrometer] class DefaultCatsMeterRegistry[F[_]: Effect](
    delegate: JavaMeterRegistry,
    blocker: Blocker
) extends CatsMeterRegistry[F] {

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
    new DefaultDistributionSummary[F](delegate.summary(name, tags.asJavaTags))
  }

}

private object DefaultCatsMeterRegistry {

  private object CollectionSizeToDouble extends ToDouble[Iterable[_]] {
    override def toDouble(value: Iterable[_]): Double = value.size.toDouble
  }

}
