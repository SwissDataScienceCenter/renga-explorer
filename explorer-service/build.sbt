organization := "ch.datascience"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"

lazy val projectName = "explorer-service"
name := projectName

lazy val root = Project(
  id   = projectName,
  base = file(".")
).dependsOn(
  core,
  serviceCommons
).enablePlugins(PlayScala)

lazy val core = RootProject(file("../graph-core"))
lazy val serviceCommons = RootProject(file("../service-commons"))

resolvers += DefaultMavenRepository

lazy val janusgraph_version = "0.1.0"

libraryDependencies += filters
libraryDependencies += "org.janusgraph" % "janusgraph-cassandra" % janusgraph_version //% Runtime

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test

import com.typesafe.sbt.packager.docker._

dockerBaseImage := "openjdk:8-jre-alpine"

dockerCommands ~= { cmds => cmds.head +: ExecCmd("RUN", "apk", "add", "--no-cache", "bash") +: cmds.tail }
