package pme123.camunda.dmn.tester.server.runner

import pme123.camunda.dmn.tester.shared.HandledTesterException.ConfigException
import pme123.camunda.dmn.tester.shared.TesterValue._
import pme123.camunda.dmn.tester.shared._
import zio._

import java.io.File
import scala.language.implicitConversions
import scala.util.Try

object DmnConfigHandler {

  def read(configPath: Seq[String]): IO[HandledTesterException, DmnConfig] =
    read((os.pwd / configPath).toIO)

  def read(file: File): IO[HandledTesterException, DmnConfig] =
    hocon
      .loadConfig(file)


  def write(
      dmnConfig: DmnConfig,
      path: List[String]
  ): IO[HandledTesterException, Unit] = {
    ZIO
      .fromTry(Try(osPath(path) / dmnConfig.dmnConfigPathStr))
      .mapError { ex =>
        ex.printStackTrace()
        ConfigException(ex.getMessage)
      }
      .tap(f => print(s"Config Path write: ${f.toIO.getAbsolutePath}"))
      .flatMap { configFile =>
        hocon
          .writeConfig(dmnConfig)
          .mapError(failureMsg => ConfigException(failureMsg))
          .flatMap(c =>
            ZIO
              .fromTry(Try(os.write(configFile, c, createFolders = true)))
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
  ): IO[HandledTesterException, Unit] = {
    ZIO
      .fromTry(Try(osPath(path) / dmnConfig.dmnConfigPathStr))
      .mapError(
        { ex =>
          ex.printStackTrace()
          ConfigException(ex.getMessage)
        }
      )
      .tap(f => print(s"Config Path to delete: ${f.toIO.getAbsolutePath}"))
      .map(p => os.remove(p))

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
      case DateValue(value)    => Some(value)
      case NullValue           => Some(null)
    }

  lazy val stringValue: ConfigDescriptor[TesterValue] =
    string(
      TesterValue.fromAny,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case NullValue          => Some(NullValue.constant)
          case DateValue(value)   => Some(value)
          case StringValue(value) => Some(value)
          case _                  => None
        }
    )

  private lazy val bigDecimalValue: ConfigDescriptor[TesterValue] =
    bigDecimal(
      NumberValue.apply,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case NumberValue(value) => Some(value)
          case _                  => None
        }
    )

  private lazy val booleanValue: ConfigDescriptor[TesterValue] =
    boolean(
      BooleanValue.apply,
      (bd: TesterValue) => // specific as otherwise there is a ClassCastExceptio
        bd match {
          case BooleanValue(value) => Some(value)
          case _                   => None
        }
    ).asInstanceOf[ConfigDescriptor[TesterValue]]

  private val testerValue: _root_.zio.config.ConfigDescriptor[TesterValue] =
    bigDecimalValue orElse booleanValue orElse stringValue

  private lazy val testerInput: ConfigDescriptor[TesterInput] =
    (string("key") zip boolean("nullValue").default(false) zip nested("values")(
      list(testerValue)
    ) zip int("id").optional).to[TesterInput]

  lazy val testResult: ConfigDescriptor[TestResult] =
    (int("rowIndex") zip
      map("outputs")(testerValue)).to[TestResult]

  private lazy val testCases: ConfigDescriptor[TestCase] =
    (map("inputs")(testerValue) zip
      list("results")(testResult)).to[TestCase]

  private lazy val testerData: ConfigDescriptor[TesterData] =
    (list("inputs")(testerInput) zip
      list("variables")(testerInput) zip
      list("testCases")(testCases).default(List.empty)).to[TesterData]

  lazy val dmnConfig: ConfigDescriptor[DmnConfig] =
    (string("decisionId") zip nested("data")(testerData) zip nested("dmnPath")(
      list(string)
    ) zip boolean("isActive").default(false) zip boolean("testUnit")
      .default(true)).to[DmnConfig]

  def loadConfig(configFile: File): IO[HandledTesterException, DmnConfig] = {
    print(s"load file $configFile") *>
      ZIO
        .succeed(
          TypesafeConfigSource
            .fromHoconFile(configFile)
        )
        .flatMap(readConfig)
  }

  private[runner] def readConfig(configSource: ConfigSource) =
    read(dmnConfig from configSource)
      .mapError(exc => ConfigException(s"Problem reading Config Source: ${exc.getMessage()}"))

  private[runner] def writeConfig(config: DmnConfig): IO[String, String] =
    ZIO
      .fromEither(write(dmnConfig, config))
      .map(_.toHoconString)
}
