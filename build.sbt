import BuildSupport.ScalaVersions._

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / scalacOptions ++=
  List(
    // warnings
    "-Wconf:msg=While parsing annotations in:silent"
  )

lazy val settingsCommon = List(
  organization := "com.avast",
  scalaVersion := scala213,
  licenses := List("MIT" -> url(s"https://github.com/avast/cats-micrometer/blob/${version.value}/LICENSE")),
  description := "Library for Micrometer written in Cats-Effect"
)

lazy val root = project
  .in(file("."))
  .aggregate(api, core, avastMetrics)
  .settings(settingsCommon)
  .settings(
    name := "cats-micrometer",
    publish / skip := true
  )

lazy val api = project
  .in(file("api"))
  .settings(settingsCommon)
  .settings(
    name := "cats-micrometer-api",
    libraryDependencies ++= Seq(Dependencies.Micrometer.core)
  )

lazy val core = project
  .in(file("core"))
  .settings(settingsCommon)
  .settings(
    name := "cats-micrometer-core",
    libraryDependencies ++= Seq(
      Dependencies.Avast.metricsCatsEffect,
      Dependencies.Micrometer.core,
      Dependencies.Scalatest.scalaTest % Test,
      Dependencies.Slf4j.api
    )
  )
  .dependsOn(api)

lazy val avastMetrics = project
  .in(file("avast-metrics"))
  .settings(settingsCommon)
  .settings(
    name := "cats-micrometer-avast-metrics",
    libraryDependencies ++= Seq(
      Dependencies.Avast.metricsCore,
      Dependencies.Avast.metricsCatsEffect,
      Dependencies.Micrometer.statsd % Test,
      Dependencies.Scalatest.scalaTest % Test
    )
  )
  .dependsOn(core)
