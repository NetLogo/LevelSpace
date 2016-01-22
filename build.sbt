name := "LevelsSpace"

scalaVersion := "2.11.7"

enablePlugins(NetLogoExtension)

netLogoExtName  := "ls"

netLogoClassManager := "LevelsSpace"

javaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

val netLogoJarURL =
  Option(System.getProperty("netlogo.jar.url")).getOrElse("https://s3.amazonaws.com/ccl-artifacts/NetLogo-hexy-fd7cd755.jar")

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
      "org.nlogo" % "NetLogo" % "6.0-PREVIEW-12-15" from netLogoJarURL,
      "org.nlogo" % "NetLogo-tests" % "6.0-PREVIEW-12-15" % "test" from testsUrl))
}

netLogoJarsOrDependencies

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.ow2.asm" % "asm-all" % "5.0.3" % "test"
)

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles <++= baseDirectory { base =>
  Seq(base / "extensions")
}

