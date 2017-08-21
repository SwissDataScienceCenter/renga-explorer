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
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"
name := "renga-explorer"

lazy val root = (project in file("."))
  .enablePlugins(
    PlayScala
  )

resolvers += "SDSC Snapshots" at "https://testing.datascience.ch:18081/repository/maven-snapshots/"

lazy val renga_version = "0.1.0-SNAPSHOT"
libraryDependencies += "ch.datascience" %% "renga-graph-core" % renga_version
libraryDependencies += "ch.datascience" %% "renga-commons" % renga_version exclude("org.slf4j", "slf4j-log4j12") exclude("org.slf4j", "slf4j-nop")

lazy val rengaCommonsUri = uri(s"$rengaCommonsRepo#$rengaCommonsRef")
lazy val rengaCommonsRepo = "ssh://git@github.com/SwissDataScienceCenter/renga-commons.git"
lazy val rengaCommonsRef = "master"
lazy val commons = ProjectRef(rengaCommonsUri, "root")

lazy val janusgraph_version = "0.1.0"

libraryDependencies += filters
libraryDependencies += "org.janusgraph" % "janusgraph-cassandra" % janusgraph_version //% Runtime

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test


import com.typesafe.sbt.packager.docker._
dockerBaseImage := "openjdk:8-jre-alpine"
dockerCommands ~= { cmds => cmds.head +: ExecCmd("RUN", "apk", "add", "--no-cache", "bash") +: cmds.tail }

// Source code formatting
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

val preferences =
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference( AlignArguments,                               true  )
    .setPreference( AlignParameters,                              true  )
    .setPreference( AlignSingleLineCaseStatements,                true  )
    .setPreference( AlignSingleLineCaseStatements.MaxArrowIndent, 40    )
    .setPreference( CompactControlReadability,                    true  )
    .setPreference( CompactStringConcatenation,                   false )
    .setPreference( DanglingCloseParenthesis,                     Force )
    .setPreference( DoubleIndentConstructorArguments,             true  )
    .setPreference( DoubleIndentMethodDeclaration,                true  )
    .setPreference( FirstArgumentOnNewline,                       Force )
    .setPreference( FirstParameterOnNewline,                      Force )
    .setPreference( FormatXml,                                    true  )
    .setPreference( IndentPackageBlocks,                          true  )
    .setPreference( IndentSpaces,                                 2     )
    .setPreference( IndentWithTabs,                               false )
    .setPreference( MultilineScaladocCommentsStartOnFirstLine,    false )
    .setPreference( NewlineAtEndOfFile,                           true  )
    .setPreference( PlaceScaladocAsterisksBeneathSecondAsterisk,  false )
    .setPreference( PreserveSpaceBeforeArguments,                 false )
    .setPreference( RewriteArrowSymbols,                          false )
    .setPreference( SpaceBeforeColon,                             false )
    .setPreference( SpaceBeforeContextColon,                      true  )
    .setPreference( SpaceInsideBrackets,                          false )
    .setPreference( SpaceInsideParentheses,                       true  )
    .setPreference( SpacesAroundMultiImports,                     true  )
    .setPreference( SpacesWithinPatternBinders,                   false )

SbtScalariform.scalariformSettings ++ Seq(preferences)

libraryDependencies ++= Seq("org.scalactic" %% "scalactic" % "3.0.3",
 "org.scalatest" %% "scalatest" % "3.0.3" % "test",
"org.mockito" % "mockito-core" % "2.8.47" )

// Publishing
publishTo := {
  val nexus = "https://testing.datascience.ch:18081/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "repository/maven-snapshots/")
  else
    None //TODO
}
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
