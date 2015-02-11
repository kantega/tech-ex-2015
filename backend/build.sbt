

name := "backend"

version := "1.0"

scalaVersion := "2.11.5"

jetty()

scalacOptions ++= Seq("-implicitConversions","-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

scalacOptions in Test ++= Seq("-Yrangepos")

javaOptions := Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000")

parallelExecution in Test := false

resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= {
  val lensVersion = "1.0.1"
  val http4sVersion = "0.5.1"
  val phantomVersion = "1.2.2"
  // "com.github.julien-truffaut" %% "monocle-core" % lensVersion,
  Seq(
    "org.slf4j" % "slf4j-api" % "1.7.6",
    "ch.qos.logback" % "logback-core" % "1.1.1",
    "ch.qos.logback" % "logback-classic" % "1.1.1",
    "org.joda" % "joda-convert" % "1.4",
    "org.specs2" %% "specs2-core" % "2.4.15" % "test",
    "org.typelevel" %% "scalaz-specs2" % "0.3.0" % "test",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.1" % "test", //httpclient
    "org.http4s" % "http4s-server_2.11" % http4sVersion withSources() withJavadoc(),
    "org.http4s" % "http4s-servlet_2.11" % http4sVersion withSources() withJavadoc(),
    "org.http4s" % "http4s-dsl_2.11" % http4sVersion withSources() withJavadoc(),
    "org.http4s" % "http4s-argonaut_2.11" % http4sVersion withSources() withJavadoc(),
    "org.http4s" % "http4s-jetty_2.11" % http4sVersion % "test" withSources() withJavadoc() ,
    "com.websudos" % "phantom-dsl_2.10" % phantomVersion,
    "org.tpolecat" % "doobie-core_2.11" % "0.2.0", //database
    "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
    "com.h2database" % "h2" % "1.4.182",
    "com.zaxxer" % "HikariCP" % "2.2.5",
    "com.github.nscala-time" %% "nscala-time" % "1.8.0",
    "io.argonaut" %% "argonaut" % "6.1-M4" //json
  )
}

    