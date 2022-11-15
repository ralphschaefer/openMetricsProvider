import sbt._

object AkkaDependencies {
  lazy val akkaHttpVersion = "10.4.0"
  lazy val akkaVersion    = "2.7.0"
  lazy val akkaManagementVersion = "1.0.8"

  lazy val libs = Seq (
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  )
}

