package pme123.camunda.dmn.tester.server.zzz

import ammonite.ops._
import pme123.camunda.dmn.tester.shared.HandledTesterException.ConfigException
import pme123.camunda.dmn.tester.shared.TesterValue._
import pme123.camunda.dmn.tester.shared._
import zio._

import java.io.File
import scala.language.implicitConversions

object DmnConfigHandler {

  def read(configPath: Seq[String]): IO[ConfigException, DmnConfig] =
    read((pwd / configPath).toIO)

  def read(file: File): IO[ConfigException, DmnConfig] =
    hocon
      .loadConfig(file)
      .mapError { ex =>
        ex.printStackTrace()
        ConfigException(ex.getMessage)
      }
}

object hocon {
  import zio.config._
  import ConfigDescriptor._
  import zio.config.typesafe._

  val stringValue: ConfigDescriptor[TesterValue] =
    string(StringValue.apply, StringValue.unapply).asInstanceOf[ConfigDescriptor[TesterValue]]
  val bigDecimalValue: ConfigDescriptor[TesterValue] =
    bigDecimal(NumberValue.apply, NumberValue.unapply).asInstanceOf[ConfigDescriptor[TesterValue]]
  val booleanValue: ConfigDescriptor[TesterValue] =
    boolean(BooleanValue.apply, BooleanValue.unapply).asInstanceOf[ConfigDescriptor[TesterValue]]

  val testerInput: ConfigDescriptor[TesterInput] =
    (string("key") |@| list("values")(bigDecimalValue orElse booleanValue orElse stringValue))(TesterInput.apply, TesterInput.unapply)

  val testerData: ConfigDescriptor[TesterData] =
    list("inputs") (testerInput)(TesterData.apply, TesterData.unapply)

  val dmnConfig: ConfigDescriptor[DmnConfig] =
    (string("decisionId") |@| nested("data")(testerData) |@| list("dmnPath")(string) |@| boolean("isActive").default(false))(DmnConfig.apply, DmnConfig.unapply)

  def loadConfig(configFile: File): Task[DmnConfig] = {
    ZIO(println(s"load file $configFile")) *>
      TypesafeConfigSource.fromHoconFile(configFile)
        .flatMap(s => ZIO.fromEither(read(dmnConfig from s)))
  }
}
