package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.server.{ZDmnService => z}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.{DmnConfig, DmnEvalResult}
import zio.{IO, Runtime, Unsafe, ZIO}

class DmnService {

  def getBasePath(): String =
    run(z.basePath())

  def getConfigPaths(): Seq[String] =
    run(z.loadConfigPaths())

  def getConfigs(path: Seq[String]): Seq[DmnConfig] =
    run(z.loadConfigs(path))

  def addConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    run(
      z.addConfig(dmnConfig, path)
    )

  def updateConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    run(
      z.updateConfig(dmnConfig, path)
    )

  def deleteConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    run(
      z.deleteConfig(dmnConfig, path)
    )

  def dmnPathExists(dmnPath: String) =
    run(
      z.dmnPathExists(dmnPath)
    )

  def runTests(
      dmnConfigs: Seq[DmnConfig]
  ): Seq[Either[EvalException, DmnEvalResult]] =
    run(z.runTests(dmnConfigs))

  private val runtime = Runtime.default

  private def run[E, A](body: => IO[E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(body).getOrThrowFiberFailure()
    }
}

object DmnService extends DmnService {

}