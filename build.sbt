/*
 * Copyright 2017 - Swiss Data Science Center (SDSC)
 * A partnership between École Polytechnique Fédérale de Lausanne (EPFL) and
 * Eidgenössische Technische Hochschule Zürich (ETHZ).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

organization := "ch.datascience"
name := "graph-type-ws"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.8"

resolvers ++= Seq(
  DefaultMavenRepository,
  "SDSC Snapshots" at "https://internal.datascience.ch:8081/nexus/content/repositories/snapshots/"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  filters,
  "com.typesafe.play" %% "play-slick" % "2.1.0",
  "ch.datascience" %% "graph-type-utils" % version.value,
  "ch.datascience" %% "graph-type-manager" % version.value,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
)

lazy val initDB = taskKey[Unit]("Initialize database")

fullRunTask(initDB, Runtime, "init.Main")
