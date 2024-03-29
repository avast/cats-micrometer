package com.avast.micrometer

import cats.effect.IO
import com.avast.micrometer.api.Tag
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.*

class TestCatsMeterRegistryTest extends AnyFlatSpec with Matchers {
  it should "return counter count" in {
    val registry = new TestCatsMeterRegistry[IO]()
    registry.getCounterCount("counterName", Tag("key", "value")) shouldBe 0

    val io = registry.counter("counterName", Tag("key", "value")).increment(42)
    registry.getCounterCount("counterName", Tag("key", "value")) shouldBe 0

    io.unsafeRunSync()
    registry.getCounterCount("counterName", Tag("key", "value")) shouldBe 42

    io.unsafeRunSync()
    registry.getCounterCount("counterName", Tag("key", "value")) shouldBe 84
  }

  it should "return timer stats" in {
    val registry = new TestCatsMeterRegistry[IO]()
    registry.getTimerStats("timerName", Tag("key", "value")).count shouldBe 0

    val io = registry.timer("timerName", Tag("key", "value")).record(42.millis)
    registry.getTimerStats("timerName", Tag("key", "value")).count shouldBe 0

    io.unsafeRunSync()
    registry.getTimerStats("timerName", Tag("key", "value")).count shouldBe 1
    registry.getTimerStats("timerName", Tag("key", "value")).totalTime shouldBe 42.millis

    io.unsafeRunSync()
    registry.getTimerStats("timerName", Tag("key", "value")).count shouldBe 2
    registry.getTimerStats("timerName", Tag("key", "value")).totalTime shouldBe 84.millis
  }

  it should "return timer pair stats" in {
    val registry = new TestCatsMeterRegistry[IO]()
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 0
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 0

    val ioSuccess = registry.timerPair("timerPairName", Tag("key", "value")).recordSuccess(42.millis)
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 0
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 0

    ioSuccess.unsafeRunSync()
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 1
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.totalTime shouldBe 42.millis
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 0
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.totalTime shouldBe 0.millis

    ioSuccess.unsafeRunSync()
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 2
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.totalTime shouldBe 84.millis
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 0
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.totalTime shouldBe 0.millis

    val ioFailure = registry.timerPair("timerPairName", Tag("key", "value")).recordFailure(99.millis)
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 2
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.totalTime shouldBe 84.millis
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 0
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.totalTime shouldBe 0.millis

    ioFailure.unsafeRunSync()
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 2
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.totalTime shouldBe 84.millis
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 1
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.totalTime shouldBe 99.millis

    ioFailure.unsafeRunSync()
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.count shouldBe 2
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).successes.totalTime shouldBe 84.millis
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.count shouldBe 2
    registry.getTimerPairStats("timerPairName", Tag("key", "value")).failures.totalTime shouldBe 198.millis
  }

  it should "timer with custom buckets" in {
    val registry = new TestCatsMeterRegistry[IO]()
    val timer = registry.timer("timerName", Seq(100.millis, 200.millis, 400.millis, 800.millis), Tag("key", "value"))
    timer.record(42.millis).unsafeRunSync()
    timer.record(500.millis).unsafeRunSync()

    val histogram = registry.getTimerSnapshot("timerName", Tag("key", "value"))

    histogram.count() shouldBe 2
    histogram.histogramCounts().size shouldBe 4
    histogram.histogramCounts().map(_.count()).toSeq shouldBe Seq(1, 1, 1, 2)
  }

  it should "return summary stats" in {
    val registry = new TestCatsMeterRegistry[IO]()
    registry.getSummaryStats("summaryName", Tag("key", "value")).count shouldBe 0

    val io = registry.summary("summaryName", Tag("key", "value")).record(42.9)
    registry.getSummaryStats("summaryName", Tag("key", "value")).count shouldBe 0

    io.unsafeRunSync()
    registry.getSummaryStats("summaryName", Tag("key", "value")).count shouldBe 1
    registry.getSummaryStats("summaryName", Tag("key", "value")).total shouldBe 42.9

    io.unsafeRunSync()
    registry.getSummaryStats("summaryName", Tag("key", "value")).count shouldBe 2
    registry.getSummaryStats("summaryName", Tag("key", "value")).total shouldBe 85.8
  }

  it should "return summary stats and histogram" in {
    val registry = new TestCatsMeterRegistry[IO]()
    val name = "histogramSummary"
    val percentiles = Array(0.5, 0.75, 1d)
    val expectedValues = Map(0.5 -> 42.9, 0.75 -> 52.9, 1d -> 62.9)
    val digitsOfPrecision = 3
    val scale = 1d

    val config = DistributionStatisticConfig
      .builder()
      .percentiles(percentiles*)
      .percentilePrecision(digitsOfPrecision)
      .percentilesHistogram(true)
      .build()

    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).count shouldBe 0

    val io = registry.summary(name, config, scale, Tag("key", "value")).record(42.9)
    val io2 = registry.summary(name, config, scale, Tag("key", "value")).record(52.9)
    val io3 = registry.summary(name, config, scale, Tag("key", "value")).record(62.9)
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).count shouldBe 0

    io.unsafeRunSync()
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).count shouldBe 1
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).total shouldBe 42.9

    io.unsafeRunSync()
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).count shouldBe 2
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).total shouldBe 85.8

    io2.unsafeRunSync()
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).count shouldBe 3
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).total shouldBe 138.7

    io3.unsafeRunSync()
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).count shouldBe 4
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).total shouldBe 201.6

    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).percentileValues().map(_.percentile) shouldBe percentiles
    registry.getHistogramSnapshot(name, config, scale, Tag("key", "value")).percentileValues().foreach { tuple =>
      expectedValues(tuple.percentile()) - 0.1 < tuple.value() &&
      expectedValues(tuple.percentile()) + 0.1 > tuple.value()
    }
  }

  it should "return gauge value" in {
    val registry = new TestCatsMeterRegistry[IO]()
    registry.getGaugeValue("gaugeName", Tag("key", "value")) shouldBe 0

    val mutableCollection = new ArrayBuffer[Int]()

    registry.gaugeCollectionSize("gaugeName", Tag("key", "value"))(mutableCollection)
    registry.getGaugeValue("gaugeName", Tag("key", "value")) shouldBe 0

    mutableCollection += 1

    registry.getGaugeValue("gaugeName", Tag("key", "value")) shouldBe 1

    for (_ <- 2 to 42) {
      mutableCollection += 1
    }

    registry.getGaugeValue("gaugeName", Tag("key", "value")) shouldBe 42
  }
}
