package com.avast.micrometer.api

trait Gauge[F[_]] {
  def value: F[Double]
}
