#!/usr/bin/env amm

// repo for DMN Tester
interp.repositories() ++= Seq(
  coursierapi.MavenRepository.of("https://dl.bintray.com/pme123/maven")
)

// this compiles the script in 2 parts (add first resolver and then runs the script)

@

import $ivy.`pme123::camunda-dmn-tester-server:0.8.0-SNAPSHOT`
import pme123.camunda.dmn.tester.server.runner._

// add here your comma separated list with Paths you have your DMN Tester Configs
val configPaths = "/dmnConfigs"

sys.props.addOne("TESTER_CONFIG_PATHS",  configPaths)

DmnUnitTestGenerator(UnitTestGeneratorConfig(
  "pme123.camunda.dmn.tester.demo",
  List("..", "server", "target", "generated-tests")
)).run()