package com.avast.micrometer.avastMetrics

import com.avast.metrics.TimerPairImpl
import com.avast.metrics.api._
import com.avast.metrics.core.TimerHelper
import io.micrometer.core.instrument.{
  Counter => MCounter,
  DistributionSummary => MDistributionSummary,
  Gauge => MGauge,
  Meter => MMeter,
  MeterRegistry,
  Timer => MTimer
}
import org.slf4j.LoggerFactory

import java.math.{BigDecimal, BigInteger}
import java.time.Duration
import java.util.concurrent._
import java.util.function.Supplier

class MicrometerToMonitorAdapter(val meterRegistry: MeterRegistry, prefixNames: Seq[String] = Nil) extends Monitor {
  private val logger = LoggerFactory.getLogger(getClass)

  override def named(name: String): Monitor = {
    new MicrometerToMonitorAdapter(meterRegistry, prefixNames :+ escapeName(name))
  }

  override def named(name: String, name2: String, names: String*): Monitor = {
    new MicrometerToMonitorAdapter(meterRegistry, prefixNames ++ (Seq(name, name2) ++ names).map(escapeName))
  }

  override def getName: String = prefixNames.mkString(".")

  override def newMeter(name: String): Meter = new Meter with MicrometerMetric {
    val getName: String = makeName(name)
    val underlying: MCounter = meterRegistry.counter(getName)

    override def mark(): Unit = underlying.increment()
    override def mark(n: Long): Unit = underlying.increment(n.toDouble)
    override def count(): Long = underlying.count().toLong
  }

  override def newCounter(name: String): Counter = new Counter with MicrometerMetric {
    val getName: String = makeName(name)
    val underlying: MCounter = meterRegistry.counter(getName)

    override def inc(): Unit = underlying.increment()
    override def inc(n: Long): Unit = underlying.increment(n.toDouble)
    override def dec(): Unit = logger.debug("Decrementing a counter is not supported by Micrometer!")
    override def dec(n: Int): Unit = logger.debug("Decrementing a counter is not supported by Micrometer!")
    override def count(): Long = underlying.count().toLong
  }

  override def newTimer(name: String): Timer = new Timer with MicrometerMetric {
    val getName: String = makeName(name)
    val underlying: MTimer = meterRegistry.timer(getName)

    private val clock = meterRegistry.config().clock()

    override def start(): Timer.TimeContext = new Timer.TimeContext {
      private val start = clock.monotonicTime()
      override def stop(): Unit = underlying.record(Duration.ofNanos(clock.monotonicTime() - start))
    }

    override def update(duration: Duration): Unit = underlying.record(duration)
    override def time[T](operation: Callable[T]): T = underlying.recordCallable(operation)

    override def time[T](operation: Callable[T], failureTimer: Timer): T = {
      TimerHelper.time(operation, this, failureTimer)
    }

    override def timeAsync[T](operation: Callable[CompletableFuture[T]], executor: Executor): CompletableFuture[T] = {
      TimerHelper.timeAsync(operation, this, executor)
    }

    override def timeAsync[T](operation: Callable[CompletableFuture[T]], failureTimer: Timer, executor: Executor): CompletableFuture[T] = {
      TimerHelper.timeAsync(operation, this, failureTimer, executor)
    }

    override def count(): Long = underlying.count()
  }

  override def newTimerPair(name: String): TimerPair = {
    new TimerPairImpl(
      newTimer(Naming.defaultNaming().successTimerName(name)),
      newTimer(Naming.defaultNaming().failureTimerName(name))
    )
  }

  override def newGauge[T](name: String, gauge: Supplier[T]): Gauge[T] = new Gauge[T] with MicrometerMetric {
    val getName: String = makeName(name)
    val underlying: MMeter = MGauge.builder(getName, gauge, (g: Supplier[T]) => convert(g.get())).register(meterRegistry)

    override def getValue: T = gauge.get()
  }

  override def newGauge[T](name: String, replaceExisting: Boolean, gauge: Supplier[T]): Gauge[T] = new Gauge[T] with MicrometerMetric {
    val getName: String = makeName(name)

    private val builder = MGauge.builder(getName, gauge, (g: Supplier[T]) => convert(g.get()))

    if (replaceExisting) {
      // Here we create the gauge... and remove it immediately. The point is that an existing gauge is returned (if there's some) and we need
      // its ID for the removal. No, you can't get valid ID without this - at least not for all MeterRegistry.
      // The worst thing that can happen is that the gauge is created, deleted and created again - in a single moment.
      //
      // This code is definitely NOT nice. But it works. Change it if you know how - the functionality is covered by tests...
      meterRegistry.remove(builder.register(meterRegistry))
    }

    val underlying: MMeter = builder.register(meterRegistry)

    override def getValue: T = gauge.get()
  }

  override def newHistogram(name: String): Histogram = new Histogram with MicrometerMetric {
    val getName: String = makeName(name)
    val underlying: MDistributionSummary = meterRegistry.summary(getName)

    override def update(value: Long): Unit = underlying.record(value.toDouble)
  }

  override def remove(metric: Metric): Unit = {
    metric match {
      case mm: MicrometerMetric =>
        meterRegistry.remove(mm.underlying)
        ()

      case _ => // weird... but what can we do ¯\_(ツ)_/¯
    }
  }

  override def close(): Unit = meterRegistry.close()

  private def makeName(name: String): String = {
    (prefixNames :+ escapeName(name)).mkString(".")
  }

  private def escapeName(name: String): String = {
    name.replace('.', '-')
  }

  // adapted from Dropwizard Graphite reporter
  @SuppressWarnings(Array("scalafix:DisableSyntax.throw", "scalafix:Disable.Any"))
  private def convert(o: Any): Double = {
    o match {
      case fl: Float           => fl.doubleValue
      case d: Double           => d
      case b: Byte             => b.longValue.toDouble
      case sh: Short           => sh.longValue.toDouble
      case integer: Integer    => integer.longValue.toDouble
      case l: Long             => l.toDouble
      case integer: BigInteger => integer.doubleValue
      case decimal: BigDecimal => decimal.doubleValue
      case bool: Boolean       => if (bool) 1d else 0d

      case _ => throw new IllegalArgumentException(s"Cannot create Gauge with value of type ${o.getClass.getName}")
    }
  }
}

private trait MicrometerMetric {
  def getName: String
  def underlying: MMeter
}
