package com.avast.cbs.micrometer

trait DistributionSummary[F[_]] {
  def record(value: Double): F[Unit]
}
