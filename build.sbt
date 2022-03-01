import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension)
enablePlugins(ExtensionDocumentationPlugin)

scalaVersion := "2.12.12"

name       := "LevelSpace"
version    := "2.3.2"
isSnapshot := true

netLogoExtName      := "ls"
netLogoClassManager := "org.nlogo.ls.LevelSpace"
netLogoTarget       := NetLogoExtension.directoryTarget(baseDirectory.value)
netLogoZipSources   := false
netLogoVersion      := "6.2.2"
netLogoTestExtras   += (baseDirectory.value / "test")

scalaSource in Compile := baseDirectory.value / "src" / "main"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0"
)
