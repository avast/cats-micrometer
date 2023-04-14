import sbt._

object Dependencies {

  object Avast {
    private val version = "3.0.2"
    val metricsCore = "com.avast.metrics" % "metrics-core" % version
    val metricsCatsEffect = ("com.avast.metrics" %% "metrics-cats-effect-2" % version).cross(CrossVersion.for3Use2_13)
  }

  object Micrometer {
    private val version = "1.6.5"
    val core = "io.micrometer" % "micrometer-core" % version
    val statsd = "io.micrometer" % "micrometer-registry-statsd" % version
  }

  object Typelevel {
    private val version = "2.5.4"
    val catsEffect = "org.typelevel" %% "cats-effect" % version
  }

  object Scalatest {
    private val version = "3.2.15"
    val scalaTest = "org.scalatest" %% "scalatest" % version
  }

  object Slf4j {
    private val version = "1.7.36"
    val api = "org.slf4j" % "slf4j-api" % version
  }

}
