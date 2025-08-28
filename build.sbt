import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension, ExtensionDocumentationPlugin)

scalaVersion := "3.7.0"

name       := "LevelSpace"
version    := "2.4.2"
isSnapshot := true

netLogoExtName      := "ls"
netLogoClassManager := "org.nlogo.ls.LevelSpace"
netLogoVersion      := "7.0.0-RC1-e8801f2"
netLogoTestExtras   += (baseDirectory.value / "test")

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test / scalaSource    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature", "-release", "11", "-Wunused:linted")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"
)
