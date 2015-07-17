name := "LevelsSpace"

scalaVersion := "2.11.7"

retrieveManaged := true

javaSource  in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

libraryDependencies ++= Seq(
  "com.google.guava"  % "guava"         % "18.0",
  "org.nlogo"         % "NetLogo"       % "5.3-LevelSpace"          from "http://ccl.northwestern.edu/devel/NetLogo-5.3-LevelSpace-3a6b9b4.jar",
  "org.nlogo"         % "NetLogo-tests" % "5.3-LevelSpace" % "test" from "http://ccl.northwestern.edu/devel/NetLogo-tests-5.3-LevelSpace-3a6b9b4.jar",
  "org.scalatest"     %% "scalatest"    % "2.2.4" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "asm"               % "asm-all"       % "3.3.1" % "test"
)

artifactName := { (_, _, _) => "ls.jar" }

netLogoClassManager := "LevelsSpace"

netLogoExtName      := "ls"

netLogoZipSources   := false

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles <++= baseDirectory { base =>
  Seq(base / "extensions")
}

