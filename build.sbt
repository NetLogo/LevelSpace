name := "LevelsSpace"

scalaVersion := "2.9.2"

retrieveManaged := true

javaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "org.nlogo" % "NetLogo" % "5.1.0" from "http://ccl.northwestern.edu/netlogo/5.1.0/NetLogo.jar",
  "org.nlogo" % "NetLogo-tests" % "5.1.0" % "test" from "http://ccl.northwestern.edu/netlogo/5.1.0/NetLogo-tests.jar",
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "asm" % "asm-all" % "3.3.1" % "test"
)

artifactName := { (_, _, _) => "ls.jar" }

packageOptions := Seq(
  Package.ManifestAttributes(
    ("Extension-Name", "ls"),
    ("Class-Manager", "LevelsSpace"),
    ("NetLogo-Extension-API-Version", "5.0")))

packageBin in Compile := {
  val jar = (packageBin in Compile).value
  val target = baseDirectory.value / "extensions" / "ls"
  val s = streams.value
  IO.createDirectory(target)
  IO.copyFile(jar, target / "ls.jar")
  val classpath = (dependencyClasspath in Runtime).value
  val libraryJarPaths =
    classpath.files.filter{path =>
      path.getName.endsWith(".jar") &&
      !path.getName.startsWith("scala-library")}
  for(path <- libraryJarPaths) {
    IO.copyFile(path, target /  path.getName)
  }
  jar
}

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles <++= baseDirectory { base =>
  Seq(base / "extensions")
}

