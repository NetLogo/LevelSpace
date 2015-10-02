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

libraryDependencies ++= Seq(
  "com.google.guava"  % "guava"         % "18.0",
  "org.scalatest"     %% "scalatest"    % "2.2.4" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "asm"               % "asm-all"       % "3.3.1" % "test"
)

netLogoZipSources   := false

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles <++= baseDirectory { base =>
  Seq(base / "extensions")
}

val netLogoJarsOrDependencies =
  Option(System.getProperty("netlogo.jar.url"))
    .orElse(Some("http://ccl.northwestern.edu/devel/NetLogo-5.3.levelspace-7f479a4.jar"))
    .map { url =>
      import java.io.File
      import java.net.URI
      val testsUrl = url.replaceFirst("NetLogo", "NetLogo-tests")
      if (url.startsWith("file:"))
        (Seq(new File(new URI(url)), new File(new URI(testsUrl))), Seq())
      else
        (Seq(), Seq(
          "org.nlogo" % "NetLogo" % "5.3.levelspace" from url,
          "org.nlogo" % "NetLogo-tests" % "5.3.levelspace" % "test" from testsUrl))
    }.get

unmanagedJars in Compile ++= netLogoJarsOrDependencies._1

libraryDependencies ++= netLogoJarsOrDependencies._2

