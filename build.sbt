enablePlugins(org.nlogo.build.NetLogoExtension)

scalaVersion := "2.11.7"

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

javaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

retrieveManaged := true

netLogoExtName := "ls"

netLogoClassManager := "LevelSpace"

netLogoTarget :=
  org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value)

val netLogoJarURL =
  Option(System.getProperty("netlogo.jar.url")).getOrElse("https://s3.amazonaws.com/ccl-artifacts/NetLogo-c210708.jar")

val netLogoJarsOrDependencies = {
  import java.io.File
  import java.net.URI
  val urlSegments = netLogoJarURL.split("/")
  val lastSegment = urlSegments.last.replaceFirst("NetLogo", "NetLogo-tests")
  val testsUrl = (urlSegments.dropRight(1) :+ lastSegment).mkString("/")
  if (netLogoJarURL.startsWith("file:"))
    Seq(unmanagedJars in Compile ++= Seq(
      new File(new URI(netLogoJarURL)), new File(new URI(testsUrl))))
  else
    Seq(libraryDependencies ++= Seq(
      "org.nlogo" % "NetLogo" % "6.0-M4-SNAPSHOT" from netLogoJarURL,
      "org.nlogo" % "NetLogo-tests" % "6.0-M4-SNAPSHOT" % "test" from testsUrl))
}

netLogoJarsOrDependencies

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.ow2.asm" % "asm-all" % "5.0.3" % "test",
  "com.google.guava"  % "guava"         % "18.0"
)

packageBin in Compile := {
  val jar = (packageBin in Compile).value
  val base = baseDirectory.value
  IO.copyFile(jar, base / "ls.jar")
  jar
}

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles ++= {
  val base = baseDirectory.value
  val lsDir = base / "extensions" / "ls"
  Seq(base / "ls.jar")
}
