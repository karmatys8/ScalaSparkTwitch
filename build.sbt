name := "Simple Project"

version := "1.0"

scalaVersion := "3.4.2"

libraryDependencies ++= Seq(
  ("org.apache.spark" %% "spark-sql" % "3.5.1" % "provided").cross(CrossVersion.for3Use2_13)
)

Compile / run / fork := true

Compile / run / javaOptions ++= Seq(
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
)

Compile / run := Defaults.runTask(Compile / fullClasspath, Compile / run / mainClass, Compile / run / runner).evaluated
