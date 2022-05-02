package com.avast.micrometer

import cats.effect.Sync
import cats.syntax.all._
import com.avast.micrometer.api.Timer
import io.micrometer.core.instrument.{Timer => Delegate}

import java.time.{Duration => JavaDuration}
import java.util.concurrent.{Callable, TimeUnit}
import scala.concurrent.duration.Duration
import cats.effect.MonadCancel

private[micrometer] class DefaultTimer[F[_]: Sync](delegate: Delegate, clock: F[Long]) extends Timer[F] {

  private val F = Sync[F]

  override def record(duration: JavaDuration): F[Unit] = F.delay(delegate.record(duration))

  override def record(duration: Duration): F[Unit] = {
    F.delay(delegate.record(duration.toNanos, TimeUnit.NANOSECONDS))
  }

  override def wrap[A](block: => A): F[A] = {
    F.delay {
      delegate
        .wrap(new Callable[A] {
          override def call(): A = block
        })
        .call()
    }
  }

  override def wrap[A](f: F[A]): F[A] = {
    MonadCancel[F, Throwable].bracket(clock)(_ => f)(start => clock.map(end => delegate.record(end - start, TimeUnit.NANOSECONDS)))
  }

  override def count: F[Double] = F.delay(delegate.count().toDouble)

  override def totalTime(unit: TimeUnit): F[Double] = F.delay(delegate.totalTime(unit))

}
