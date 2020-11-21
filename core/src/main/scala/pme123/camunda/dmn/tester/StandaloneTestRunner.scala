package pme123.camunda.dmn.tester

import pme123.camunda.dmn.tester.TestRunner
import zio.Runtime

object StandaloneTestRunner extends App {

  private lazy val runtime = Runtime.default

  def standalone(config: RunnerConfig): Unit =
    runtime.unsafeRun(TestRunner.runApp(config))
}
