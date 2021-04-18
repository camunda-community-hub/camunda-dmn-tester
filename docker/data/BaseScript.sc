#!/usr/bin/env amm

import ammonite.ops._

// this compiles the script in 2 parts (add first resolver and then runs the script)

private implicit val workDir: Path = {
  val wd = pwd
  println(s"Working Directory: $wd")
  wd
}

val packagePath = pwd / "testerVersion"
val testerVersion: String = read.lines(packagePath).head
interp.load.ivy("io.github.pme123" %% "camunda-dmn-tester-server" % testerVersion)
