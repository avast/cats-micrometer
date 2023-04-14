package com.avast.micrometer.avastMetrics

import io.micrometer.core.instrument.config.{MeterFilter, NamingConvention}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import io.micrometer.core.instrument.{Clock, Meter}
import io.micrometer.statsd.*
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}

import java.time.Duration
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

@SuppressWarnings(Array("scalafix:DisableSyntax.asInstanceOf"))
class MicrometerToMonitorAdapterTest extends AnyFlatSpec with Matchers with Eventually {
  final val Prefix = "test.prefix."

  it should "make nested monitors" in {
    val monitor = new MicrometerToMonitorAdapter(new SimpleMeterRegistry())

    monitor.getName shouldBe ""
    monitor.named("level1").getName shouldBe "level1"
    monitor.named("level1", "level2").getName shouldBe "level1.level2"
    monitor.named("level1", "level2", "level3").getName shouldBe "level1.level2.level3"
    monitor.getName shouldBe ""

    monitor.named("1.2.3.4").getName shouldBe "1-2-3-4"
    monitor.named("domain.com", "1.2.3.4").getName shouldBe "domain-com.1-2-3-4"
  }

  it should "produce nested metrics" in {
    val monitor = new MicrometerToMonitorAdapter(new SimpleMeterRegistry())

    val subMonitor = monitor.named("level1", "level2")
    subMonitor.getName shouldBe "level1.level2"
    monitor.getName shouldBe ""

    monitor.newCounter("theCounter").getName shouldBe "theCounter"
    monitor.newMeter("theMeter").getName shouldBe "theMeter"
    monitor.newTimer("theTimer").getName shouldBe "theTimer"
    monitor.newGauge("theGauge", () => 42).getName shouldBe "theGauge"

    subMonitor.newCounter("theCounter").getName shouldBe "level1.level2.theCounter"
    subMonitor.newMeter("theMeter").getName shouldBe "level1.level2.theMeter"
    subMonitor.newTimer("theTimer").getName shouldBe "level1.level2.theTimer"
    subMonitor.newGauge("theGauge", () => 42).getName shouldBe "level1.level2.theGauge"
  }

  it should "report counter value" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    val counter = monitor.newCounter("theCounter")
    counter.inc()
    counter.inc(42)
    counter.inc(43)
    counter.inc(44)

    counter.count() shouldBe 1 + 42 + 43 + 44

    eventually(timeout = timeout(Span(200, Millis))) {
      received.toSeq shouldBe Seq(
        s"${Prefix}theCounter.statistic.count:1|c",
        s"${Prefix}theCounter.statistic.count:42|c",
        s"${Prefix}theCounter.statistic.count:43|c",
        s"${Prefix}theCounter.statistic.count:44|c"
      )
    }
  }

  it should "report meter value" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    val meter = monitor.newMeter("theMeter")
    meter.mark()
    meter.mark(42)
    meter.mark(43)
    meter.mark(44)

    meter.count() shouldBe 1 + 42 + 43 + 44

    eventually(timeout = timeout(Span(200, Millis))) {
      received.toSeq shouldBe Seq(
        s"${Prefix}theMeter.statistic.count:1|c",
        s"${Prefix}theMeter.statistic.count:42|c",
        s"${Prefix}theMeter.statistic.count:43|c",
        s"${Prefix}theMeter.statistic.count:44|c"
      )
    }
  }

  it should "report timer value" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    val timer = monitor.newTimer("theTimer")
    timer.update(Duration.ofMillis(42))
    timer.update(Duration.ofMillis(43))
    timer.update(Duration.ofMillis(44))

    timer.count() shouldBe 3

    eventually(timeout = timeout(Span(200, Millis))) {
      received.toSeq shouldBe Seq(
        s"${Prefix}theTimer:42|ms",
        s"${Prefix}theTimer:43|ms",
        s"${Prefix}theTimer:44|ms"
      )
    }
  }

  it should "report timer-pair value" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    val timerPair = monitor.named("nested").newTimerPair("theTimerPair")
    timerPair.update(Duration.ofMillis(42))
    timerPair.update(Duration.ofMillis(43))
    timerPair.updateFailure(Duration.ofMillis(44))
    timerPair.updateFailure(Duration.ofMillis(45))

    eventually(timeout = timeout(Span(200, Millis))) {
      received.toSeq shouldBe Seq(
        s"${Prefix}nested.theTimerPairSuccesses:42|ms",
        s"${Prefix}nested.theTimerPairSuccesses:43|ms",
        s"${Prefix}nested.theTimerPairFailures:44|ms",
        s"${Prefix}nested.theTimerPairFailures:45|ms"
      )
    }
  }

  it should "report histogram value" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    val histogram = monitor.newHistogram("theHistogram")
    histogram.update(42)
    histogram.update(43)
    histogram.update(44)

    eventually(timeout = timeout(Span(200, Millis))) {
      received.toSeq shouldBe Seq(
        s"${Prefix}theHistogram:42|h",
        s"${Prefix}theHistogram:43|h",
        s"${Prefix}theHistogram:44|h"
      )
    }
  }

  it should "report gauge value" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    monitor.newGauge("theGauge", () => 42)

    eventually(timeout = timeout(Span(500, Millis))) {
      received.toSeq.take(3) shouldBe Seq(
        s"${Prefix}theGauge.statistic.value:42|g",
        s"${Prefix}theGauge.statistic.value:42|g",
        s"${Prefix}theGauge.statistic.value:42|g"
      )
    }
  }

  it should "report replace existing gauge" in {
    val received = mutable.Buffer.empty[String]

    val monitor = newMonitor() { line =>
      //println(s"Received: $line")
      received += line
    }

    monitor.newGauge("theGauge", () => 42)
    Thread.sleep(150) // gauge reporting period is 100ms
    monitor.newGauge("theGauge", replaceExisting = true, () => 52)

    eventually(timeout = timeout(Span(500, Millis))) {
      received.toSeq.distinct shouldBe Seq(
        s"${Prefix}theGauge.statistic.value:42|g",
        s"${Prefix}theGauge.statistic.value:52|g"
      )
    }
  }

  it should "remove metric from registry when requested" in {
    val monitor = newMonitor() { _ => () }

    monitor.meterRegistry.getMeters.size() shouldBe 0

    val counter = monitor.newCounter("theCounter")
    assert(monitor.meterRegistry.getMeters.asScala.exists(_.getId == counter.asInstanceOf[MicrometerMetric].underlying.getId))
    monitor.remove(counter)
    assert(!monitor.meterRegistry.getMeters.asScala.exists(_.getId == counter.asInstanceOf[MicrometerMetric].underlying.getId))

    val meter = monitor.newMeter("theMeter")
    assert(monitor.meterRegistry.getMeters.asScala.exists(_.getId == meter.asInstanceOf[MicrometerMetric].underlying.getId))
    monitor.remove(meter)
    assert(!monitor.meterRegistry.getMeters.asScala.exists(_.getId == meter.asInstanceOf[MicrometerMetric].underlying.getId))

    val timer = monitor.newTimer("theTimer")
    assert(monitor.meterRegistry.getMeters.asScala.exists(_.getId == timer.asInstanceOf[MicrometerMetric].underlying.getId))
    monitor.remove(timer)
    assert(!monitor.meterRegistry.getMeters.asScala.exists(_.getId == timer.asInstanceOf[MicrometerMetric].underlying.getId))

    val gauge = monitor.newGauge("theGauge", () => 42)
    assert(monitor.meterRegistry.getMeters.asScala.exists(_.getId == gauge.asInstanceOf[MicrometerMetric].underlying.getId))
    monitor.remove(gauge)
    assert(!monitor.meterRegistry.getMeters.asScala.exists(_.getId == gauge.asInstanceOf[MicrometerMetric].underlying.getId))

    val histogram = monitor.newHistogram("theHistogram")
    assert(monitor.meterRegistry.getMeters.asScala.exists(_.getId == histogram.asInstanceOf[MicrometerMetric].underlying.getId))
    monitor.remove(histogram)
    assert(!monitor.meterRegistry.getMeters.asScala.exists(_.getId == histogram.asInstanceOf[MicrometerMetric].underlying.getId))

    monitor.meterRegistry.getMeters.size() shouldBe 0
  }

  private def newMonitor(clock: Clock = Clock.SYSTEM)(consume: String => Unit): MicrometerToMonitorAdapter = {
    val registry = StatsdMeterRegistry
      .builder(new CustomStatsdConfig())
      .clock(clock)
      .lineSink(consume(_))
      .nameMapper(HierarchicalNameMapper.DEFAULT)
      .build

    registry.config().namingConvention(NamingConvention.dot)
    registry.config().meterFilter(new PrefixMeterFilter(Prefix))

    new MicrometerToMonitorAdapter(registry)
  }
}

private class CustomStatsdConfig() extends StatsdConfig {
  override val flavor: StatsdFlavor = StatsdFlavor.ETSY
  override val enabled: Boolean = true
  override val protocol: StatsdProtocol = StatsdProtocol.UDP
  override val pollingFrequency: java.time.Duration = java.time.Duration.ofMillis(100)
  override val step: java.time.Duration = java.time.Duration.ofMillis(10000)
  override val publishUnchangedMeters: Boolean = true
  override val buffered: Boolean = false

  @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
  override def get(key: String): String = null

}

private[micrometer] class PrefixMeterFilter(prefix: String) extends MeterFilter {
  override def map(id: Meter.Id): Meter.Id = id.withName(s"$prefix${id.getName}")
}
