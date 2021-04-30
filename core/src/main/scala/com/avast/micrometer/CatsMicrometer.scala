package com.avast.micrometer

import cats.effect.{Blocker, Effect}
import com.avast.micrometer.api.CatsMeterRegistry
import io.micrometer.core.instrument.{MeterRegistry => JavaMeterRegistry}

object CatsMicrometer {
  def wrapRegistry[F[_]: Effect](
      registry: JavaMeterRegistry,
      blocker: Blocker
  ): CatsMeterRegistry[F] = {
    new DefaultCatsMeterRegistry(registry, blocker)
  }
}
