import org.nlogo.build.NetLogoExtension

enablePlugins(org.nlogo.build.NetLogoExtension)

name := "LevelSpace"

scalaVersion := "2.11.7"

retrieveManaged := true

netLogoClassManager := "LevelSpace"

netLogoExtName      := "ls"

javaSource  in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

javacOptions ++= Seq("-g", "-deprecation", "-Xlint:all", "-Xlint:-serial", "-Xlint:-path",
                      "-encoding", "us-ascii")

libraryDependencies ++= Seq(
  "com.google.guava"  % "guava"         % "18.0",
  "org.scalatest"     %% "scalatest"    % "2.2.4" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.ow2.asm"       % "asm-all"       % "5.0.3" % "test"
)

netLogoZipSources   := false

val moveToLSDir = taskKey[Unit]("move relevant files to LS directory")

moveToLSDir := {
  IO.createDirectory(baseDirectory.value / "extensions" / "ls")
  (baseDirectory.value * "*.jar").get.foreach { f =>
    IO.copyFile(f, baseDirectory.value / "extensions" / "ls" / f.getName)
  }
}

test in Test := {
  (packageBin in Compile).value
  moveToLSDir.value
  (test in Test).value
}

cleanFiles <++= baseDirectory { base =>
  Seq(base / "extensions")
}

val netLogoJarsOrDependencies =
  Option(System.getProperty("netlogo.jar.url"))
    .orElse(Some("https://s3.amazonaws.com/ccl-artifacts/NetLogo-6.0-constructionism-preview.jar"))
    .map { url =>
      import java.io.File
      import java.net.URI
      val urlSegments = url.split("/")
      val lastTestSegment = urlSegments.last.replaceFirst("NetLogo", "NetLogo-tests")
      val testsUrl = (urlSegments.dropRight(1) :+ lastTestSegment).mkString("/")
      if (url.startsWith("file:"))
        Seq(
          unmanagedJars in Compile += new File(new URI(url)),
          unmanagedJars in Test += new File(new URI(testsUrl)))
      else
        Seq(
          libraryDependencies ++= Seq(
            "org.nlogo" % "NetLogo" % "6.0.constructionism" from url,
            "org.nlogo" % "NetLogo-tests" % "6.0.constructionism" % "test" from testsUrl))
    }.get

netLogoJarsOrDependencies

netLogoTarget         := NetLogoExtension.directoryTarget(baseDirectory.value)
