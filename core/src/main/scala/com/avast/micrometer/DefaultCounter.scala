package com.avast.micrometer

import cats.effect.Sync
import com.avast.micrometer.api.Counter
import io.micrometer.core.instrument.Counter as Delegate

private[micrometer] class DefaultCounter[F[_]: Sync](delegate: Delegate) extends Counter[F] {

  private val F = Sync[F]

  override def increment: F[Unit] = increment(1.0)
  override def increment(amount: Double): F[Unit] = F.delay(delegate.increment(amount))
  override def count: F[Double] = F.delay(delegate.count())

}
