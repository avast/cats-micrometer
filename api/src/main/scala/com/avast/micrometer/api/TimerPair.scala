package com.avast.micrometer.api

import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

trait TimerPair[F[_]] {
  def recordSuccess(duration: JavaDuration): F[Unit]
  def recordSuccess(duration: Duration): F[Unit]
  def recordFailure(duration: JavaDuration): F[Unit]
  def recordFailure(duration: Duration): F[Unit]

  def wrap[A](block: => A): F[A]
  def wrap[A](f: F[A]): F[A]

  def count: F[Double]
  def countSuccesses: F[Double]
  def countFailures: F[Double]

  def totalTime(unit: TimeUnit): F[Double]
  def totalTimeSuccesses(unit: TimeUnit): F[Double]
  def totalTimeFailures(unit: TimeUnit): F[Double]
}
