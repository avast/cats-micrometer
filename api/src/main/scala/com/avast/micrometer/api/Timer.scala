package com.avast.micrometer.api

import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

trait Timer[F[_]] {
  def record(duration: JavaDuration): F[Unit]

  def record(duration: Duration): F[Unit]

  def timed[A](block: => A): F[A]

  def timed[A](f: F[A]): F[A]

  def count: F[Double]

  def totalTime(unit: TimeUnit): F[Double]
}
