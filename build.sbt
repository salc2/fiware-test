name := "fiware-tests"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-stream" % "2.4.14",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.0")
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "0.3"
libraryDependencies += "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0"
