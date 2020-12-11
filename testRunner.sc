#!/usr/bin/env amm

/** HOW TO USE
  * see README.md
  */

// only used for tmp dmn-scala
interp.repositories() ++= Seq(
  coursierapi.MavenRepository.of(
    "file://" + java.lang.System.getProperties
      .get("user.home") + "/.m2/repository/"
  ),
  coursierapi.MavenRepository.of("https://dl.bintray.com/pme123/maven")
)

// this compiles the script in 2 parts (add first resolver and then runs the script)

@

import $ivy.`pme123::camunda-dmn-tester:0.2.0`
import pme123.camunda.dmn.tester._

StandaloneTestRunner.standalone(RunnerConfig(List("dmnTester", "dmn-configs")))
