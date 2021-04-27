package com.avast.micrometer

import cats.effect.Sync
import com.avast.micrometer.api.DistributionSummary
import io.micrometer.core.instrument.{DistributionSummary => Delegate}

private[micrometer] class DefaultDistributionSummary[F[_]: Sync](delegate: Delegate) extends DistributionSummary[F] {

  private val F = Sync[F]

  override def record(value: Double): F[Unit] = {
    F.delay(delegate.record(value))
  }
}
