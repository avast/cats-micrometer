package com.avast.micrometer

import cats.effect.{Bracket, ExitCase, Sync}
import cats.syntax.all.*
import com.avast.micrometer.api.{Timer, TimerPair}

import java.time.Duration as JavaDuration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*
import scala.util.control.NonFatal

private[micrometer] class DefaultTimerPair[F[_]: Sync](successes: Timer[F], failures: Timer[F], clock: F[Long]) extends TimerPair[F] {

  private val F = Sync[F]

  override def recordSuccess(duration: JavaDuration): F[Unit] = successes.record(duration)

  override def recordSuccess(duration: Duration): F[Unit] = {
    successes.record(duration)
  }

  override def recordFailure(duration: JavaDuration): F[Unit] = failures.record(duration)

  override def recordFailure(duration: Duration): F[Unit] = {
    failures.record(duration)
  }

  override def wrap[A](block: => A): F[A] = {
    clock.flatMap { start =>
      try {
        val result = block
        clock.flatTap(end => successes.record((end - start).nanos)).map { _ =>
          result
        }
      } catch {
        case NonFatal(e) =>
          clock.flatTap(end => failures.record((end - start).nanos)) *>
            F.raiseError(e)
      }
    }
  }

  override def wrap[A](f: F[A]): F[A] = {
    Bracket[F, Throwable].bracketCase(clock)(_ => f) {
      case (start, ExitCase.Completed) => clock.flatMap(end => successes.record((end - start).nanos))
      case (start, ExitCase.Error(_))  => clock.flatMap(end => failures.record((end - start).nanos))
      case _                           => F.unit
    }
  }

  override def count: F[Double] = {
    for {
      s <- countSuccesses
      f <- countFailures
    } yield {
      s + f
    }
  }

  override def countSuccesses: F[Double] = successes.count
  override def countFailures: F[Double] = failures.count

  override def totalTime(unit: TimeUnit): F[Double] = {
    for {
      s <- totalTimeSuccesses(unit)
      f <- totalTimeFailures(unit)
    } yield {
      s + f
    }
  }

  override def totalTimeSuccesses(unit: TimeUnit): F[Double] = successes.totalTime(unit)

  override def totalTimeFailures(unit: TimeUnit): F[Double] = failures.totalTime(unit)

}
