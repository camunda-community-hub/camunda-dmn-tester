package pme123.camunda.dmn.tester.server.zzz


import ammonite.ops._

import java.io.File
import pme123.camunda.dmn.tester.server.HandledTesterException
import pme123.camunda.dmn.tester.shared.TesterValue._
import pme123.camunda.dmn.tester.shared._
import zio._

import scala.language.implicitConversions

object DmnConfigHandler {

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

object hocon {
  import zio.config._
  import zio.config.magnolia.DeriveConfigDescriptor._
  import zio.config.typesafe._
  import zio.config._, ConfigDescriptor._, ConfigSource._

  val stringValue: ConfigDescriptor[TesterValue] =
    (string)(StringValue.apply, StringValue.unapply).asInstanceOf[ConfigDescriptor[TesterValue]]
  val bigDecimalValue: ConfigDescriptor[TesterValue] =
    (bigDecimal)(NumberValue.apply, NumberValue.unapply).asInstanceOf[ConfigDescriptor[TesterValue]]
  val booleanValue: ConfigDescriptor[TesterValue] =
    (boolean)(BooleanValue.apply, BooleanValue.unapply).asInstanceOf[ConfigDescriptor[TesterValue]]

  val testerInput: ConfigDescriptor[TesterInput] =
    (string("key") |@| list("values")(bigDecimalValue orElse booleanValue orElse( stringValue)))(TesterInput.apply, TesterInput.unapply)

  val testerData: ConfigDescriptor[TesterData] =
    (list("inputs") (testerInput))(TesterData.apply, TesterData.unapply)

  val dmnConfig: ConfigDescriptor[DmnConfig] =
    (string("decisionId") |@| nested("data")(testerData) |@| list("dmnPath")(string))(DmnConfig.apply, DmnConfig.unapply)

  def loadConfig(configFile: File): Task[DmnConfig] = {
    ZIO(println(s"load file $configFile")) *>
      TypesafeConfigSource.fromHoconFile(configFile)
        .flatMap(s => ZIO.fromEither(read(dmnConfig from s)))
  }
}
