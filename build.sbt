lazy val settingsCommon = avastPureBundleAggregatedSettings ++ List(
  organization := "com.avast.cbs",
  scalaVersion := avastScala213Version,
  ThisBuild / version := sys.props.get("avast.version").getOrElse(version.value),
  missinglinkExcludedDependencies += moduleFilter(name = "micrometer-core")
)

lazy val root = project
  .in(file("."))
  .aggregate(api, core)
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
    libraryDependencies ++= Seq(Dependencies.micrometerCore)
  )

lazy val core = project
  .in(file("core"))
  .settings(settingsCommon)
  .settings(
    name := "cats-micrometer-core",
    libraryDependencies ++= Seq(
      Dependencies.catsEffect,
      Dependencies.micrometerCore,
      Dependencies.scalaTest % Test
    )
  )
  .dependsOn(api)
