name := "web-scheduler"

version := "0.234"

resolvers += "theatr.us" at "http://repo.theatr.us"

libraryDependencies ++= Seq(
  "org.codehaus.groovy" % "groovy-all" % "2.2.2",
  "us.theatr" %% "akka-quartz" % "0.3.0-SNAPSHOT",
  "com.twitter" %% "util-eval" % "6.13.2",
  anorm,
  cache
)     

play.Project.playScalaSettings
