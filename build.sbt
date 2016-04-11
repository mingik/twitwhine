name := """twitwhine"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.3" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
