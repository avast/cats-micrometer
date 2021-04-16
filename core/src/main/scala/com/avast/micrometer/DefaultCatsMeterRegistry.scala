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

  override def counter(name: String, tags: Iterable[Tag]): Counter[F] = {
    new DefaultCounter(delegate.counter(name, tags.asJavaTags))
  }

  override def counter(name: String, tags: Tag*): Counter[F] = {
    counter(name, tags)
  }

  override def gauge[A: ToDouble](name: String, tags: Iterable[Tag])(retrieveValue: F[A]): Gauge[F] = {
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

  override def gauge[A: ToDouble](name: String, tags: Tag*)(retrieveValue: F[A]): Gauge[F] = {
    gauge(name, tags)(retrieveValue)
  }

  override def gaugeCollectionSize[A <: Iterable[_]](name: String, tags: Iterable[Tag], collection: A): Gauge[F] = {
    gauge(name, tags)(F.pure(collection))(CollectionSizeToDouble)
  }

  override def timer(name: String, tags: Iterable[Tag]): Timer[F] = {
    new DefaultTimer(delegate.timer(name, tags.asJavaTags), clock)
  }

  override def timer(name: String, tags: Tag*): Timer[F] = {
    timer(name, tags)
  }

  override def timerPair(name: String, tags: Iterable[Tag]): TimerPair[F] = {
    new DefaultTimerPair(
      timer(name, Seq(Tag("type", "success")) ++ tags),
      timer(name, Seq(Tag("type", "failure")) ++ tags),
      clock
    )
  }

  override def timerPair(name: String, tags: Tag*): TimerPair[F] = {
    timerPair(name, tags)
  }

  override def summary(name: String, tags: Iterable[Tag]): DistributionSummary[F] = {
    new DefaultDistributionSummary[F](delegate.summary(name, tags.asJavaTags))
  }

  override def summary(name: String, tags: Tag*): DistributionSummary[F] = {
    summary(name, tags)
  }
}

private object DefaultCatsMeterRegistry {

  private object CollectionSizeToDouble extends ToDouble[Iterable[_]] {
    override def toDouble(value: Iterable[_]): Double = value.size.toDouble
  }

}
