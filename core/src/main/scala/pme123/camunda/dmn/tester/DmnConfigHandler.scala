package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops._
import pme123.camunda.dmn.HandledTesterException
import zio.IO

import scala.language.implicitConversions

object DmnConfigHandler extends App {

  def read(configPath: Seq[String]): IO[HandledTesterException, DmnConfig] =
    read((pwd / configPath).toIO)

  def read(file: File): IO[HandledTesterException, DmnConfig] =
    hocon
      .loadConfig(file)
      .mapError { ex =>
        ex.printStackTrace()
        HandledTesterException(ex.getMessage)
      }
}
