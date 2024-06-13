name := "Simple Project"

version := "1.0"

scalaVersion := "3.4.2"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M14",
  "com.softwaremill.sttp.client4" %% "monix" % "4.0.0-M14",
  "com.softwaremill.sttp.client4" %% "circe" % "4.0.0-M14",
  "io.circe" %% "circe-generic" % "0.14.7",
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.6"
)

Compile / run / fork := true

Compile / run / javaOptions ++= Seq(
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
)

Compile / run := Defaults.runTask(Compile / fullClasspath, Compile / run / mainClass, Compile / run / runner).evaluated
