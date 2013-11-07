organization := "com.ansvia.digapidoc"

name := "digapidoc"

description := ""

version := "0.3-beta"

scalaVersion := "2.9.2"

parallelExecution in Test := false

mainClass := Some("com.ansvia.digapidoc.Digapidoc")

resolvers ++= Seq(
	"Sonatype Releases" at "https://oss.sonatype.org/content/groups/scala-tools",
	"typesafe repo"   at "http://repo.typesafe.com/typesafe/releases",
	"Ansvia release repo" at "http://scala.repo.ansvia.com/releases",
	"Ansvia snapshot repo" at "http://scala.repo.ansvia.com/nexus/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
    "org.specs2" % "specs2_2.9.2" % "1.12.4.1" % "test",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "org.streum" %% "configrity-core" % "1.0.0",
    "com.ansvia" % "ansvia-commons" % "0.0.8",
    "commons-io" % "commons-io" % "2.0.1",
    "eu.henkelmann" % "actuarius_2.9.2" % "0.2.6"
)

//EclipseKeys.withSource := true


publishTo <<= version { (v:String) =>
    val ansviaRepo = "http://scala.repo.com.ansvia.digapidoc.com/nexus"
    if(v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at ansviaRepo + "/content/repositories/snapshots")
    else
        Some("releases" at ansviaRepo + "/content/repositories/releases")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

crossPaths := false

pomExtra := (
  <url>http://com.ansvia.digapidoc.com</url>
  <developers>
    <developer>
      <id>anvie</id>
      <name>Robin Sy</name>
      <url>http://www.mindtalk.com/u/robin</url>
    </developer>
  </developers>)


seq(ProguardPlugin.proguardSettings :_*)

proguardOptions += keepMain("com.ansvia.digapidoc.Digapidoc")

proguardOptions ++= Seq("-dontnote",
    "-dontwarn", "-ignorewarnings",
//    "-dontoptimize",
//    "-dontshrink",
    "-keep interface scala.ScalaObject",
    "-keep class com.ansvia.*",
    "-keep class com.ansvia.commons.logging.Slf4jLogger",
    "-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }",
    "-keep class ch.qos.logback.*")


