package com.avast.micrometer

import io.micrometer.core.instrument._

import java.util.concurrent.TimeUnit

private object MetricsInitializer {
  def initCounter(c: Counter): Unit = {
    c.increment(1)
  }

  def initTimer(t: Timer): Unit = {
    t.record(0, TimeUnit.NANOSECONDS)
  }

  def initSummary(s: DistributionSummary): Unit = {
    s.record(0)
  }

  def initLongTaskTimer(t: LongTaskTimer): Unit = {
    t.record((() => ()): Runnable)
  }
}
