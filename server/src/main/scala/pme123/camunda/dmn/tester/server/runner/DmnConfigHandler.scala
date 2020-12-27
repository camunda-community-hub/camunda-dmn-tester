package pme123.camunda.dmn.tester.server.runner

import ammonite.ops
import ammonite.ops._
import pme123.camunda.dmn.tester.shared.HandledTesterException.ConfigException
import pme123.camunda.dmn.tester.shared.TesterValue._
import pme123.camunda.dmn.tester.shared._
import zio._
import zio.console.Console

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

  def write(
      dmnConfig: DmnConfig,
      path: List[String]
  ): ZIO[Console, ConfigException, Unit] = {
    ZIO(osPath(path) / s"${dmnConfig.decisionId}.conf")
      .tap(f => console.putStrLn(s"Config Path: ${f.toIO.getAbsolutePath}"))
      .mapError { ex =>
        ex.printStackTrace()
        ConfigException(ex.getMessage)
      }
      .flatMap { configFile =>
        hocon
          .writeConfig(dmnConfig)
          .bimap(
            failureMsg => ConfigException(failureMsg),
            ops.write(configFile, _)
          )
      }
  }
}

object hocon {
  import zio.config._
  import ConfigDescriptor._
  import zio.config.typesafe._

  def testerValueToJson(testerValue: TesterValue): Some[Any] =
    testerValue match {
      case NumberValue(value)  => Some(value)
      case StringValue(value)  => Some(value)
      case BooleanValue(value) => Some(value)
      case ValueSet(set)       => Some(set.flatMap(testerValueToJson))
    }

  val stringValue: ConfigDescriptor[TesterValue] =
    string(StringValue.apply, StringValue.unapply)
      .asInstanceOf[ConfigDescriptor[TesterValue]]

  val bigDecimalValue: ConfigDescriptor[TesterValue] =
    bigDecimal(
      NumberValue.apply,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case NumberValue(value) => Some(value)
          case other              => None
        }
    ).asInstanceOf[ConfigDescriptor[TesterValue]]

  val booleanValue: ConfigDescriptor[TesterValue] =
    boolean(
      BooleanValue.apply,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case BooleanValue(value) => Some(value)
          case _                   => None
        }
    ).asInstanceOf[ConfigDescriptor[TesterValue]]

  val testerInput: ConfigDescriptor[TesterInput] =
    (string("key") |@| list("values")(
      bigDecimalValue orElse booleanValue orElse stringValue
    ))(TesterInput.apply, TesterInput.unapply)

  val testerData: ConfigDescriptor[TesterData] =
    list("inputs")(testerInput)(TesterData.apply, TesterData.unapply)

  val dmnConfig: ConfigDescriptor[DmnConfig] =
    (string("decisionId") |@| nested("data")(testerData) |@| list("dmnPath")(
      string
    ) |@| boolean("isActive")
      .default(false))(DmnConfig.apply, DmnConfig.unapply)

  def loadConfig(configFile: File): Task[DmnConfig] = {
    ZIO(println(s"load file $configFile")) *>
      ZIO
        .fromEither(
          TypesafeConfigSource
            .fromHoconFile(configFile)
        )
        .flatMap(readConfig)
  }

  private[runner] def readConfig(configSource: ConfigSource) =
    ZIO.fromEither(read(dmnConfig from configSource))

  private[runner] def writeConfig(config: DmnConfig): IO[String, String] =
    ZIO
      .fromEither(write(dmnConfig, config))
      .map(_.toHoconString)
}
