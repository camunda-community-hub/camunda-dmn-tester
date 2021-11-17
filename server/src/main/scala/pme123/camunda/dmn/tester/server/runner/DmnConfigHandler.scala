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
          .mapError(failureMsg => ConfigException(failureMsg))
          .flatMap(c =>
            ZIO(ops.write(configFile, c))
              .mapError(ex => {
                ConfigException(
                  s"Could not write Config '${dmnConfig.decisionId}'\n${ex.getClass.getName}: ${ex.getMessage}"
                )
              })
          )
      }
  }

  def delete(
      dmnConfig: DmnConfig,
      path: List[String]
  ): ZIO[Console, ConfigException, Unit] = {
    ZIO(osPath(path) / s"${dmnConfig.decisionId}.conf")
      .tap(f => console.putStrLn(s"Config Path: ${f.toIO.getAbsolutePath}"))
      .bimap(
        { ex =>
          ex.printStackTrace()
          ConfigException(ex.getMessage)
        },
        p => ops.rm(p)
      )
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
      case NullValue => Some(null)
    }

  val stringValue: ConfigDescriptor[TesterValue] =
    string(
      TesterValue.fromAny,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case NullValue => Some(NullValue.constant)
          case StringValue(value) => Some(value)
          case _                   => None
        }
    )

  val bigDecimalValue: ConfigDescriptor[TesterValue] =
    bigDecimal(
      NumberValue.apply,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case NumberValue(value) => Some(value)
          case _              => None
        }
    )

  val booleanValue: ConfigDescriptor[TesterValue] =
    boolean(
      BooleanValue.apply,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case BooleanValue(value) => Some(value)
          case _                   => None
        }
    ).asInstanceOf[ConfigDescriptor[TesterValue]]

  val testerValue: _root_.zio.config.ConfigDescriptor[TesterValue] =
    bigDecimalValue orElse booleanValue orElse stringValue

  val testerInput: ConfigDescriptor[TesterInput] =
    (string("key") |@| boolean("nullValue").default(false) |@| list("values")(testerValue))(
      TesterInput.apply,
      TesterInput.unapply
    )

  val testResult: ConfigDescriptor[TestResult] =
    (int("rowIndex") |@|
      map("outputs")(testerValue))(TestResult.apply, TestResult.unapply)

  val testCases: ConfigDescriptor[TestCase] =
    (map("inputs")(testerValue) |@|
      list("results")(testResult))(TestCase.apply, TestCase.unapply)

  val testerData: ConfigDescriptor[TesterData] =
    (list("inputs")(testerInput) |@|
      list("variables")(testerInput) |@|
      list("testCases")(testCases).default(List.empty))(
      TesterData.apply,
      TesterData.unapply
    )

  val dmnConfig: ConfigDescriptor[DmnConfig] =
    (string("decisionId") |@| nested("data")(testerData) |@| list("dmnPath")(
      string
    ) |@| boolean("isActive") |@| boolean("testUnit")
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
