name := "jmPayments"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.10"
val slf4jVersion = "1.7.30"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion
)


libraryDependencies += "org.slf4j" % "slf4j-api" % slf4jVersion
libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jVersion

libraryDependencies += "com.typesafe" % "config" % "1.4.1"

