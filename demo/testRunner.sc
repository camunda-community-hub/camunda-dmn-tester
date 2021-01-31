import $file.BaseScript

import pme123.camunda.dmn.tester.server.HttpServer

// add here your comma separated list with Paths you have your DMN Tester Configs
val configPaths = "/dmnConfigs"

sys.props.addOne("TESTER_CONFIG_PATHS",  configPaths)

HttpServer.main(Array.empty[String])
