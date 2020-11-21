package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops._
import zio.Task

import scala.language.implicitConversions

object DmnConfigHandler extends App {

  def read(configPath: Seq[String]): Task[DmnConfig] =
    read((pwd / configPath).toIO)

  def read(file: File): Task[DmnConfig] =
    hocon.loadConfig(file)
}
