ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "zoned-balancer-demo",
    libraryDependencies ++= Seq(
      "com.twitter"  %% "finagle-http"         % "22.12.0",
      "com.twitter"  %% "finagle-partitioning" % "22.12.0",
      "de.vandermeer" % "asciitable"           % "0.3.2"
    )
  )
