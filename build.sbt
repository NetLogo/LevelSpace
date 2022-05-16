import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension, ExtensionDocumentationPlugin)

scalaVersion := "2.12.12"

name       := "LevelSpace"
version    := "2.3.3"
isSnapshot := true

netLogoExtName      := "ls"
netLogoClassManager := "org.nlogo.ls.LevelSpace"
netLogoZipSources   := false
netLogoVersion      := "6.2.2"
netLogoTestExtras   += (baseDirectory.value / "test")

scalaSource in Compile := baseDirectory.value / "src" / "main"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-feature")

val asmVers = "7.0"

libraryDependencies ++= Seq(
<<<<<<< HEAD
  "com.google.guava" % "guava" % "18.0"
=======
  "com.google.guava"          % "guava"         % "18.0"
, "com.google.code.findbugs"  % "jsr305"        % "3.0.0"
, "org.parboiled"            %% "parboiled"     % "2.1.3"
, "org.scalatest"            %% "scalatest"     % "3.0.0"  % "test"
, "org.picocontainer"         % "picocontainer" % "2.13.6" % "test"
, "com.typesafe"              % "config"        % "1.3.1"  % "test"
, "org.ow2.asm"               % "asm"           % asmVers  % "test"
, "org.ow2.asm"               % "asm-commons"   % asmVers  % "test"
, "org.ow2.asm"               % "asm-util"      % asmVers  % "test"
, "commons-codec"             % "commons-codec" % "1.10"   % "test"
>>>>>>> c8cc080 (java 11: newInstance() -> getDeclaredConstructor().newInstance())
)
