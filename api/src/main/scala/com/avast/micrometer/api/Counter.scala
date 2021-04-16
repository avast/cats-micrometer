package com.avast.micrometer.api

trait Counter[F[_]] {
  def increment: F[Unit]

  def increment(amount: Double): F[Unit]

  def count: F[Double]
}
