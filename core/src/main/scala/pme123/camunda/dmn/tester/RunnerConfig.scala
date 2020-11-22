package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops._
import zio.UIO

case class RunnerConfig(
    basePath: List[String]
)
object RunnerConfig {

  val defaultBasePath = List("core", "src", "test", "resources", "dmn-configs")
  val defaultConfig: RunnerConfig = RunnerConfig(defaultBasePath)

  //TODO add support for config file
  def config = {
    UIO(defaultConfig)
  }

}
