object BuildSupport {
  object ScalaVersions {
    lazy val scala212 = "2.12.17"
    lazy val scala213 = "2.13.10"
    lazy val scala3 = "3.2.2"
    lazy val supportedScalaVersions = List(scala212, scala213, scala3)
  }


  /*
  lazy val micrositeSettings = Seq(
    micrositeName := "cats-micrometer",
    micrositeDescription := "FP wrapper over Micrometer library",
    // micrositeAuthor := "",
    micrositeGithubOwner := "avast",
    micrositeGithubRepo := "cats-micrometer",
    micrositeUrl := "https://avast.github.io",
    micrositeDocumentationUrl := "api/latest/com/avast/cats-micrometer/",
    micrositeBaseUrl := "/cats-micrometer",
    micrositeFooterText := None,
    micrositeGitterChannel := false,
    micrositeTheme := "pattern",
    mdocIn := file("site") / "docs",
    mdocVariables := Map(
      "VERSION" -> {
        if (!isSnapshot.value) {
          version.value
        } else {
          previousStableVersion.value.getOrElse("latestVersion")
        }

      },
      "CE2_LATEST_VERSION" -> "0.14.0",
      "CE3_LATEST_VERSION" -> {
        if (!isSnapshot.value) {
          version.value
        } else {
          previousStableVersion.value.getOrElse("latestVersion")
        }
      },
      "CATS_VERSION" -> Cats.core.revision,
      "CATS_EFFECT_VERSION" -> Cats.effect.revision
    ),
    mdocAutoDependency := false,
    micrositeDataDirectory := file("site"),
    ScalaUnidoc / siteSubdirName := "api/latest",
    addMappingsToSiteDir(
      ScalaUnidoc / packageDoc / mappings,
      ScalaUnidoc / siteSubdirName
    )
  )*/
}
