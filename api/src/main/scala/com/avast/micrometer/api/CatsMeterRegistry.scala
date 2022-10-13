package com.avast.micrometer.api

import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.core.instrument.{MeterRegistry => JavaMeterRegistry}

import scala.concurrent.duration.FiniteDuration

trait CatsMeterRegistry[F[_]] {

  def underlying: JavaMeterRegistry

  def initIfConfigured(): F[Unit]

  def counter(name: String, tags: Tag*): Counter[F]

  def timer(name: String, tags: Tag*): Timer[F]

  def timer(name: String, serviceLevelObjectives: Seq[FiniteDuration], tags: Tag*): Timer[F]

  def timer(name: String, minimumExpectedValue: FiniteDuration, maximumExpectedValue: FiniteDuration, tags: Tag*): Timer[F]

  def timerPair(name: String, tags: Tag*): TimerPair[F]

  def gauge[A: ToDouble](name: String, tags: Tag*)(retrieveValue: F[A]): Gauge[F]

  def gaugeCollectionSize[A <: Iterable[_]](name: String, tags: Tag*)(collection: A): Gauge[F]

  /** This is a histogram. */
  def summary(name: String, tags: Tag*): DistributionSummary[F]

  def summary(name: String, summaryConfig: DistributionStatisticConfig, scale: Double, tags: Tag*): DistributionSummary[F]
}
