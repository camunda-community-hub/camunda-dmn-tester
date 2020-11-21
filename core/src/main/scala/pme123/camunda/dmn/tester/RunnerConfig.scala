package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops._
import zio.UIO

case class RunnerConfig(
    basePath: Seq[String]
)
object RunnerConfig {

  val defaultBasePath = Seq("core", "src", "test", "resources", "dmn-configs")

  //TODO add support for config file
  def config = UIO(RunnerConfig(defaultBasePath))



}
