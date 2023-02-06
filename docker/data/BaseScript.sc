#!/usr/bin/env amm

// this compiles the script in 2 parts (add first resolver and then runs the script)

private implicit val workDir: os.Path = {
  val wd = os.pwd
  println(s"Working Directory: $wd")
  wd
}

val packagePath = os.pwd / "testerVersion"
val testerVersion: String = os.read.lines(packagePath).head
interp.load.ivy("io.github.pme123" %% "camunda-dmn-tester-server" % testerVersion)
