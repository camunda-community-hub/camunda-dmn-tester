package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.server.{ZDmnService => z}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig, DmnEvalResult}
import zio.{Runtime, ZEnv, ZIO}

class DmnService extends DmnApi {

  override def getBasePath(): String =
    run(z.basePath())

  override def getConfigPaths(): Seq[String] =
    run(z.loadConfigPaths())

  override def getConfigs(path: Seq[String]): Seq[DmnConfig] =
    run(z.loadConfigs(path))

  override def addConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    run(
      z.addConfig(dmnConfig, path)
    )

  override def updateConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    run(
      z.updateConfig(dmnConfig, path)
    )

  override def deleteConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    run(
      z.deleteConfig(dmnConfig, path)
    )

  override def runTests(
      dmnConfigs: Seq[DmnConfig]
  ): Seq[Either[EvalException, DmnEvalResult]] =
    run(z.runTests(dmnConfigs))

  private val runtime = Runtime.default
  private def run[E, A](body: => ZIO[ZEnv, E, A]): A =
    runtime.unsafeRun(body)
}
