lazy val settingsCommon = avastPureBundleAggregatedSettings ++ List(
  organization := "com.avast.cbs",
  scalaVersion := avastScala213Version,
  ThisBuild / version := sys.props.get("avast.version").getOrElse(version.value)
)

lazy val root = project
  .in(file("."))
  .aggregate(api)
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

addCommandAlias("lint", "; scalafmtSbtCheck; scalafmtCheckAll")
addCommandAlias("check", "; lint; +missinglinkCheck; scalafmtSbtCheck; scalafmtCheckAll; +test")
