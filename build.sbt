import org.nlogo.build.NetLogoExtension

enablePlugins(org.nlogo.build.NetLogoExtension)
enablePlugins(org.nlogo.build.ExtensionDocumentationPlugin)

netLogoExtName := "ls"

netLogoClassManager := "org.nlogo.ls.LevelSpace"

scalaVersion := "2.12.12"

netLogoTarget := NetLogoExtension.directoryTarget(baseDirectory.value)

netLogoZipSources := false

version := "2.3.1"

isSnapshot := true

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature")

libraryDependencies ++= Seq(
  "com.google.guava"          % "guava"         % "18.0"
, "com.google.code.findbugs"  % "jsr305"        % "3.0.0"
, "org.parboiled"            %% "parboiled"     % "2.1.3"
, "org.scalatest"            %% "scalatest"     % "3.0.0"  % "test"
, "org.picocontainer"         % "picocontainer" % "2.13.6" % "test"
, "com.typesafe"              % "config"        % "1.3.1"  % "test"
, "org.ow2.asm"               % "asm-all"       % "5.0.3"  % "test"
, "commons-codec"             % "commons-codec" % "1.10"   % "test"
)

val moveToLsDir = taskKey[Unit]("add all resources to LS directory")

val lsDirectory = settingKey[File]("directory that extension is moved to for testing")

lsDirectory := baseDirectory.value / "extensions" / "ls"

moveToLsDir := {
  (packageBin in Compile).value
  val testTarget = NetLogoExtension.directoryTarget(lsDirectory.value)
  testTarget.create(NetLogoExtension.netLogoPackagedFiles.value)
  val testResources = ((baseDirectory.value / "test").allPaths).filter(_.isFile)
  for (file <- testResources.get)
    IO.copyFile(file, lsDirectory.value / "test" / IO.relativize(baseDirectory.value / "test", file).get)
}

test in Test := {
  IO.createDirectory(lsDirectory.value)
  moveToLsDir.value
  (test in Test).value
  IO.delete(lsDirectory.value)
}

resolvers += "netlogo" at "https://dl.cloudsmith.io/public/netlogo/netlogo/maven/"

netLogoVersion := "6.2.0-d27b502"
