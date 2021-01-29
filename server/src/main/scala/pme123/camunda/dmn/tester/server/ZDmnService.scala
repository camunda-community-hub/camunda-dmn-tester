package pme123.camunda.dmn.tester.server

import ammonite.ops
import ammonite.ops.pwd
import org.camunda.dmn.DmnEngine
import pme123.camunda.dmn.tester.server.runner._
import pme123.camunda.dmn.tester.shared.HandledTesterException.{ConfigException, EvalException}
import pme123.camunda.dmn.tester.shared.{DmnConfig, DmnEvalResult}
import zio.console.Console
import zio.{Task, UIO, ZIO, console}

import java.io.File

object ZDmnService {
  private val configPaths = Seq(
    "/server/src/test/resources/dmn-configs"
  )
  def basePath(): Task[String] =
    ZIO(pwd.toIO.getAbsolutePath)

  def loadConfigPaths(): Task[Seq[String]] = UIO {
    val maybeConfigs = sys.props.get("TESTER_CONFIG_PATHS") orElse sys.env.get(
      "TESTER_CONFIG_PATHS"
    )
    maybeConfigs
      .map(
        _.split(",")
          .map(_.trim)
          .filter(_.nonEmpty)
          .toSeq
      )
      .getOrElse(configPaths)
  }

  def loadConfigs(
      path: Seq[String]
  ): ZIO[Console, ConfigException, Seq[DmnConfig]] = {
    for {
      dmnConfigs <- readConfigs(path.toList)
      _ <- console.putStrLn(
        s"Found ${dmnConfigs.length} DmnConfigs in ${pwd / path}"
      )
    } yield dmnConfigs
  }

  def addConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): ZIO[Console, ConfigException, Seq[DmnConfig]] =
    DmnConfigHandler.write(dmnConfig, path.toList) *>
      loadConfigs(path)

  def updateConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): ZIO[Console, ConfigException, Seq[DmnConfig]] =
    DmnConfigHandler.delete(dmnConfig, path.toList) *>
      DmnConfigHandler.write(dmnConfig, path.toList) *>
      loadConfigs(path)

  def deleteConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): ZIO[Console, ConfigException, Seq[DmnConfig]] =
    DmnConfigHandler.delete(dmnConfig, path.toList) *>
      loadConfigs(path)

  def runTests(
      dmnConfigs: Seq[DmnConfig]
  ): ZIO[Console, Nothing, Seq[Either[EvalException, DmnEvalResult]]] =
    for {
      _ <- console.putStrLn("Let's start")
      engine <- UIO(new DmnEngine())
      results <- ZIO.foreach(
        dmnConfigs
      )(dmnConfig =>
        DmnTester
          .testDmnTable(dmnConfig, engine)
          .map(Right.apply)
          .catchAll(ex => ZIO.left(ex))
      )
    } yield results

  private def readConfigs(path: List[String]) = {
    ZIO(osPath(path).toIO)
      .tap(f => console.putStrLn(s"Config Path: ${f.getAbsolutePath}"))
      .mapError { ex =>
        ex.printStackTrace()
        ConfigException(ex.getMessage)
      }
      .flatMap {
        case f if !f.exists() =>
          ZIO.fail(
            ConfigException(
              s"Your provided Config Path does not exist (${f.getAbsolutePath})."
            )
          )
        case f if !f.isDirectory =>
          ZIO.fail(
            ConfigException(
              s"Your provided Config Path is not a directory (${f.getAbsolutePath})."
            )
          )
        case file =>
          for {
            files <- getConfigFiles(file)
            e <- ZIO.foreach(files)(f =>
              DmnConfigHandler.read(ops.Path(f).toIO)
            )
          } yield e
      }
  }

  private def getConfigFiles(f: File) = ZIO {
    f.listFiles.filter(_.getName.endsWith(".conf"))
  }.mapError { ex =>
    ex.printStackTrace()
    ConfigException(ex.getMessage)
  }
}
