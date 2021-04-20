package com.avast.micrometer

import cats.effect.Sync
import com.avast.micrometer.api.DistributionSummary
import io.micrometer.core.instrument.{DistributionSummary => Delegate}

private[micrometer] class DefaultDistributionSummary[F[_]: Sync] private (delegate: Delegate) extends DistributionSummary[F] {

  private val F = Sync[F]

  override def record(value: Double): F[Unit] = {
    F.delay(delegate.record(value))
  }
}

private object DefaultDistributionSummary {
  def createAndInit[F[_]: Sync](delegate: Delegate, initStrategy: InitStrategy): DefaultDistributionSummary[F] = {
    initStrategy.init {
      delegate.record(0)
    }
    new DefaultDistributionSummary(delegate)
  }
}
