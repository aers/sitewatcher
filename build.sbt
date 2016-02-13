name := "sitewatcher"

version := "1.0"

lazy val `sitewatcher` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  specs2 % Test,
  "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "org.postgresql" % "postgresql" % "9.4.1207",
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "com.github.tminglei" %% "slick-pg" % "0.11.0",
  "com.github.tminglei" %% "slick-pg_joda-time" % "0.11.0",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P24-B3-SNAPSHOT" exclude("org.webjars", "jquery"),
  "org.webjars" % "jquery" % "2.2.0",
  "com.sksamuel.diff" % "diff" % "1.1.11"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"