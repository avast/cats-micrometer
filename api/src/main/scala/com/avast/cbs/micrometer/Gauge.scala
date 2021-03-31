package com.avast.cbs.micrometer

trait Gauge[F[_]] {
  def value: F[Double]
}
