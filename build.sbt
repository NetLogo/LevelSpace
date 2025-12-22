import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension, ExtensionDocumentationPlugin)

scalaVersion := "3.7.0"

name       := "LevelSpace"
version    := "2.4.3"
isSnapshot := true

netLogoExtName      := "ls"
netLogoClassManager := "org.nlogo.ls.LevelSpace"
netLogoVersion      := "7.0.0-2486d1e"
netLogoTestExtras   += (baseDirectory.value / "test")

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test / scalaSource    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature", "-release", "17", "-Wunused:linted")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"
)
