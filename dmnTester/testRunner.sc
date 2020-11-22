#!/usr/bin/env amm

/**
 * HOW TO USE
 * see README.md
 */

// only used for tmp dmn-scala
interp.repositories() ++= Seq(coursierapi.MavenRepository.of(
   "file://" + java.lang.System.getProperties.get("user.home") + "/.m2/repository/"
))

// this compiles the script in 2 parts (add first resolver and then runs the script)
@

import $ivy.`pme123::camunda-dmn-tester:0.1.0`
import pme123.camunda.dmn.tester._

StandaloneTestRunner.standalone(RunnerConfig(List("dmn-configs")))


