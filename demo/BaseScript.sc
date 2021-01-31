#!/usr/bin/env amm

import ammonite.ops._

// repo for DMN Tester
interp.repositories() ++= Seq(
  coursierapi.MavenRepository.of("https://dl.bintray.com/pme123/maven")
)

// this compiles the script in 2 parts (add first resolver and then runs the script)

private implicit val workDir: Path = {
  val wd = pwd
  println(s"Working Directory: $wd")
  wd
}

val packagePath = pwd / "testerVersion"
println(s"PACKAGEPATH: $packagePath")
val testerVersion: String = read.lines(packagePath).head
interp.load.ivy("pme123" %% "camunda-dmn-tester-server" % testerVersion)
