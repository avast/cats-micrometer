package com.avast.micrometer

import cats.effect.Sync
import com.avast.micrometer.api.Gauge
import io.micrometer.core.instrument.Gauge as Delegate

private[micrometer] class DefaultGauge[F[_]: Sync](delegate: Delegate) extends Gauge[F] {

  private val F = Sync[F]

  override def value: F[Double] = F.delay(delegate.value)

}
