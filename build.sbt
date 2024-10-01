val scala3Version = "3.4.3"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "transitive-closure",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    run / fork   := true, // Makes exit codes work as expected
    // fp
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % Versions.cats
    ),
    // tests
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % Versions.scalaTest
    ).map(_ % Test),
    // Scalafix
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
