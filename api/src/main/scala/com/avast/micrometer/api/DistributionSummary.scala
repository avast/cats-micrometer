package com.avast.micrometer.api

trait DistributionSummary[F[_]] {
  def record(value: Double): F[Unit]
}
