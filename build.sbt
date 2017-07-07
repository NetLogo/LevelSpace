import org.nlogo.build.NetLogoExtension

enablePlugins(org.nlogo.build.NetLogoExtension)
enablePlugins(org.nlogo.build.ExtensionDocumentationPlugin)

netLogoExtName := "ls"

netLogoClassManager := "org.nlogo.ls.LevelSpace"

scalaVersion := "2.12.0"

netLogoTarget := NetLogoExtension.directoryTarget(baseDirectory.value)

netLogoZipSources := false

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.parboiled" %% "parboiled" % "2.1.3",
  "org.ow2.asm" % "asm-all" % "5.0.3" % "test",
  "com.google.guava"  % "guava"         % "18.0",
  "com.google.code.findbugs" % "jsr305" % "3.0.0"
)

val moveToLsDir = taskKey[Unit]("add all resources to LS directory")

val lsDirectory = settingKey[File]("directory that extension is moved to for testing")

lsDirectory := baseDirectory.value / "extensions" / "ls"

moveToLsDir := {
  (packageBin in Compile).value
  val testTarget = NetLogoExtension.directoryTarget(lsDirectory.value)
  testTarget.create(NetLogoExtension.netLogoPackagedFiles.value)
  val testResources = (baseDirectory.value / "test" ***).filter(_.isFile)
  for (file <- testResources.get)
    IO.copyFile(file, lsDirectory.value / "test" / IO.relativize(baseDirectory.value / "test", file).get)
}

test in Test := {
  IO.createDirectory(lsDirectory.value)
  moveToLsDir.value
  (test in Test).value
  IO.delete(lsDirectory.value)
}

netLogoVersion := "6.0.2-M1"
