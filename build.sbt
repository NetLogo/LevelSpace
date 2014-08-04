name := "LevelsSpace"

scalaVersion := "2.9.2"

javaSource in Compile <<= baseDirectory(_ / "src")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

libraryDependencies +=
  "org.nlogo" % "NetLogo" % "5.1.0" from
    "http://ccl.northwestern.edu/netlogo/5.1.0/NetLogo.jar" 

artifactName := { (_, _, _) => "ls.jar" }

packageOptions := Seq(
  Package.ManifestAttributes(
    ("Extension-Name", "ls"),
    ("Class-Manager", "LevelsSpace"),
    ("NetLogo-Extension-API-Version", "5.0")))

packageBin in Compile <<= (packageBin in Compile, baseDirectory, streams) map {
  (jar, base, s) =>
    IO.copyFile(jar, base / "ls.jar")
    Process("pack200 --modification-time=latest --effort=9 --strip-debug " +
            "--no-keep-file-order --unknown-attribute=strip " +
            "ls.jar.pack.gz ls.jar").!!
    if(Process("git diff --quiet --exit-code HEAD").! == 0) {
      Process("git archive -o ls.zip --prefix=ls/ HEAD").!!
      IO.createDirectory(base / "ls")
      IO.copyFile(base / "ls.jar", base / "ls" / "ls.jar")
      IO.copyFile(base / "ls.jar.pack.gz", base / "ls" / "ls.jar.pack.gz")
      Process("zip ls.zip ls/ls.jar ls/ls.jar.pack.gz").!!
      IO.delete(base / "ls")
    }
    else {
      s.log.warn("working tree not clean; no zip archive made")
      IO.delete(base / "ls.zip")
    }
    jar
  }

cleanFiles <++= baseDirectory { base =>
  Seq(base / "ls.jar",
      base / "ls.jar.pack.gz",
      base / "ls.zip") }

