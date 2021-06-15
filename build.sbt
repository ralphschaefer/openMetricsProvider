version := "0.5.0"

name := "metricsProvider"

ThisBuild / organization := "my.tools"

ThisBuild / scalaVersion := "2.13.4"

libraryDependencies ++= AkkaDependencies.libs ++ Seq(
  "com.typesafe.scala-logging"    %% "scala-logging"    % "3.9.2",
  "ch.qos.logback"                 % "logback-classic"  % "1.2.3",
  "org.scalatest"                %% "scalatest"        % "3.2.9" % "test"
)