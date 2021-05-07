package com.avast.micrometer

import com.avast.metrics.api.Monitor
import com.avast.metrics.scalaapi.{Monitor => ScalaMonitor}
import com.avast.micrometer.api.CatsMeterRegistry

package object avastMetrics {
  implicit class CatsMeterRegistryExtension[F[_]](private val cmr: CatsMeterRegistry[F]) extends AnyVal {
    def asMetricsMonitor: Monitor = new MicrometerToMonitorAdapter(cmr.underlying)
    def asMetricsScalaMonitor: ScalaMonitor = ScalaMonitor(asMetricsMonitor)
  }
}
