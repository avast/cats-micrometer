lazy val settingsCommon = avastPureBundleAggregatedSettings ++ List(
  organization := "com.avast.cbs",
  scalaVersion := avastScala213Version,
  ThisBuild / version := sys.props.get("avast.version").getOrElse(version.value)
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "cats-micrometer",
    publish / skip := true
  )

addCommandAlias("lint", "; scalafmtSbtCheck; scalafmtCheckAll")
addCommandAlias("check", "; lint; +missinglinkCheck; scalafmtSbtCheck; scalafmtCheckAll; +test")
