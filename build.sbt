name := "mockability-server"
organization := "com.dnwiebe"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test"

fork in run := true
