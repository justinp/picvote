scalaVersion := "2.11.8"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.7",
  "io.spray" %% "spray-json" % "1.3.2",
  "io.spray" %% "spray-client" % "1.3.3",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3"
)


