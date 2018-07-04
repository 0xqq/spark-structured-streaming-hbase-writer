import scala.sys.process._

lazy val commonSettings = Seq(
  name := "spark-structured-streaming-hbase-writer",
  version := "1.0",
  organization := "com.github.mohamed-a-abdelaziz",
  scalaVersion := "2.11.8",
  homepage := Some(url("https://github.com/mohamed-a-abdelaziz/spark-structured-streaming-hbase-writer")),
  scmInfo := Some(ScmInfo(url("https://github.com/mohamed-a-abdelaziz/spark-structured-streaming-hbase-writer"),
    "git@github.com:mohamed-a-abdelaziz/spark-structured-streaming-hbase-writer.git")),
  developers := List(Developer("mohamedaabdelaziz",
    "mohamedaabdelaziz",
    "mohamed.abduelaziz@gmail.com",
    url("https://github.com/mohamed-a-abdelaziz"))),
  publishMavenStyle := true,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  )
)


val dependencies = Seq(
  "org.apache.hbase" % "hbase-client" % "2.0.1",
  "org.apache.hbase" % "hbase-common" % "2.0.1",
  "org.apache.spark" %% "spark-sql" % "2.3.1" % "provided",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

val startDockers = TaskKey[Unit]("start-dockers", "Start docker instances")

(startDockers in Test) := ("sh scripts/docker_setup.sh" !)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= dependencies)
  .settings(test in Test := (test in Test).dependsOn(startDockers in Test).value)
  .settings(test in assembly := (test in assembly).dependsOn(startDockers in Test).value)