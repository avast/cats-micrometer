import sbt._

object Dependencies {

  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
  val micrometerCore = "io.micrometer" % "micrometer-core" % Versions.micrometer
  val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest
  val slf4j = "org.slf4j" % "slf4j-api" % Versions.slf4j

}
