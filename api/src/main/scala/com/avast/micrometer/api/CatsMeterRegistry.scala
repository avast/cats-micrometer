package com.avast.micrometer.api

import io.micrometer.core.instrument.{MeterRegistry => JavaMeterRegistry}

trait CatsMeterRegistry[F[_]] {

  def underlying: JavaMeterRegistry

  def counter(name: String, tags: Iterable[Tag]): Counter[F]

  def counter(name: String, tags: Tag*): Counter[F]

  def timer(name: String, tags: Iterable[Tag]): Timer[F]

  def timer(name: String, tags: Tag*): Timer[F]

  def timerPair(name: String, tags: Iterable[Tag]): TimerPair[F]

  def timerPair(name: String, tags: Tag*): TimerPair[F]

  def gauge[A: ToDouble](name: String, tags: Iterable[Tag])(retrieveValue: F[A]): Gauge[F]

  def gauge[A: ToDouble](name: String, tags: Tag*)(retrieveValue: F[A]): Gauge[F]

  def gaugeCollectionSize[A <: Iterable[_]](name: String, tags: Iterable[Tag], collection: A): Gauge[F]

  /** This is a histogram. */
  def summary(name: String, tags: Iterable[Tag]): DistributionSummary[F]

  /** This is a histogram. */
  def summary(name: String, tags: Tag*): DistributionSummary[F]

}
