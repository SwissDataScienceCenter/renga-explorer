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