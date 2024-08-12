import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension, ExtensionDocumentationPlugin)

scalaVersion := "2.12.12"

name       := "LevelSpace"
version    := "2.3.4"
isSnapshot := true

netLogoExtName      := "ls"
netLogoClassManager := "org.nlogo.ls.LevelSpace"
netLogoVersion      := "6.4.0-d2e6005"
netLogoTestExtras   += (baseDirectory.value / "test")

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test / scalaSource    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature", "-release", "11")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0"
)
