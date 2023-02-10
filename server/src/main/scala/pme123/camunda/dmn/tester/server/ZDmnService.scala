package pme123.camunda.dmn.tester.server

import org.camunda.dmn.DmnEngine
import pme123.camunda.dmn.tester.server.runner._
import pme123.camunda.dmn.tester.shared.HandledTesterException.{
  ConfigException,
  DecisionDmnCreatorException,
  EvalException
}
import pme123.camunda.dmn.tester.shared.{
  DmnConfig,
  DmnEvalResult,
  HandledTesterException
}
import zio.{IO, Task, UIO, ZIO}

import java.io.File
import scala.util.Try

object ZDmnService {

  def basePath(): Task[String] =
    ZIO.fromTry(Try(os.pwd.toIO.getAbsolutePath))

  def loadConfigPaths(): Task[Seq[String]] = ZIO.succeed {
    val maybeConfigs =
      sys.props.get("TESTER_CONFIG_PATHS") orElse
        sys.env.get("TESTER_CONFIG_PATHS")
    maybeConfigs
      .map(
        _.split(",")
          .map(_.trim)
          .filter(_.nonEmpty)
          .toSeq
      )
      .getOrElse(defaultConfigPaths)
  }

  def loadConfigs(
      path: Seq[String]
  ): IO[HandledTesterException, Seq[DmnConfig]] = {
    for {
      dmnConfigs <- readConfigs(path.toList)
      _ <- print(
        s"Found ${dmnConfigs.length} DmnConfigs in ${os.pwd / path.filter(_.trim.nonEmpty)}"
      )
    } yield dmnConfigs
  }

  def updateConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): IO[HandledTesterException, Seq[DmnConfig]] =
    DmnConfigHandler.delete(dmnConfig, path.toList) *>
      DmnConfigHandler.write(dmnConfig, path.toList) *>
      loadConfigs(path)

  def deleteConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): IO[HandledTesterException, Seq[DmnConfig]] =
    DmnConfigHandler.delete(dmnConfig, path.toList) *>
      loadConfigs(path)

  def dmnPathExists(dmnPath: String): UIO[Boolean] =
    (for {
      basePath <- basePath()
      dmnExists <- ZIO.fromTry(
        Try(os.Path(s"$basePath/$dmnPath").toIO.exists())
      )
    } yield dmnExists).catchAll(ex => ZIO.succeed(false))

  /** for a DmnConfig -> Either[EvalException, DmnEvalResult]
    */
  def runTests(
      dmnConfigs: Seq[DmnConfig]
  ): IO[HandledTesterException, Seq[Either[EvalException, DmnEvalResult]]] =
    for {
      _ <- print("Let's start")
      engine <- ZIO.succeed(new DmnEngine())
      results <- ZIO.foreach(
        dmnConfigs
      )(dmnConfig =>
        DmnTester
          .testDmnTable(dmnConfig, engine)
      )
    } yield results

  def createCaseClasses(
      dmnPath: os.Path
  ): IO[DecisionDmnCreatorException, String] =
    DecisionDmnCreator(dmnPath).run()

  private def readConfigs(
      path: List[String]
  ): IO[HandledTesterException, Array[DmnConfig]] = {
    ZIO
      .fromTry(Try(osPath(path).toIO))
      .mapError { ex =>
        ex.printStackTrace()
        ConfigException(ex.getMessage)
      }
      .tap(f => print(s"Config Path: ${f.getAbsolutePath}"))
      .flatMap {
        case f if !f.exists() =>
          ZIO.succeed(Array.empty[DmnConfig])
        case f if !f.isDirectory =>
          ZIO.fail(
            ConfigException(
              s"Your provided Config Path is not a directory (${f.getAbsolutePath})."
            )
          )
        case file =>
          for {
            files <- getConfigFiles(file)
            e <- ZIO.foreach(files)(f => DmnConfigHandler.read(os.Path(f).toIO))
          } yield e
      }
  }

  private def getConfigFiles(f: File) = ZIO
    .fromTry(
      Try(
        f.listFiles
          .filter(f2 =>
            f2.getName.endsWith(".conf") && !f2.getName.startsWith(".")
          )
      )
    )
    .mapError { ex =>
      ex.printStackTrace()
      ConfigException(ex.getMessage)
    }

}
