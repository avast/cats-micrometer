package com.avast.cbs.micrometer

import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

trait Timer[F[_]] {
  def record(duration: JavaDuration): F[Unit]

  def record(duration: Duration): F[Unit]

  def wrap[A](block: => A): F[A]

  def wrap[A](f: F[A]): F[A]

  def count: F[Double]

  def totalTime(unit: TimeUnit): F[Double]
}
