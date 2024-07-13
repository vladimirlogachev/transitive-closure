val scala2Version = "2.13.14"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "transitive-closure",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala2Version,
    run / fork   := true, // Makes exit codes work as expected
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    // Scalafix
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
