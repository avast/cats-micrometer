package com.avast.micrometer

import cats.effect.Sync
import com.avast.micrometer.api.DistributionSummary
import io.micrometer.core.instrument.DistributionSummary as Delegate

private[micrometer] class DefaultDistributionSummary[F[_]: Sync](delegate: Delegate) extends DistributionSummary[F] {

  private val F = Sync[F]

  private[micrometer] def underlying: Delegate = delegate

  override def record(value: Double): F[Unit] = {
    F.delay(delegate.record(value))
  }
}
