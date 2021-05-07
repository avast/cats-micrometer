import sbt._

object Dependencies {

  val avastMetricsCore = "com.avast.metrics" % "metrics-core" % Versions.avastMetrics
  val avastMetricsScala = "com.avast.metrics" %% "metrics-scala" % Versions.avastMetrics
  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
  val micrometerCore = "io.micrometer" % "micrometer-core" % Versions.micrometer
  val micrometerStatsd = "io.micrometer" % "micrometer-registry-statsd" % Versions.micrometer
  val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest
  val slf4j = "org.slf4j" % "slf4j-api" % Versions.slf4j

}
