package pme123.camunda.dmn.tester.server

import ammonite.ops
import ammonite.ops.pwd
import org.camunda.dmn.DmnEngine
import pme123.camunda.dmn.tester.server.runner._
import pme123.camunda.dmn.tester.shared.HandledTesterException.{ConfigException, EvalException}
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig, DmnEvalResult}
import zio.{Runtime, UIO, ZIO, console}

import java.io.File

class DmnService extends DmnApi {
  private val runtime = Runtime.default
  private val configPaths = Seq(
    "/dmnTester/dmn-configs",
    "/server/src/test/resources/dmn-configs"
  )
  override def getBasePath(): String =
    pwd.toIO.getAbsolutePath

  override def getConfigPaths(): Seq[String] = {
    val maybeConfigs = sys.props.get("TESTER_CONFIG_PATHS")
    println("TESTER_CONFIG_PATHS: " + maybeConfigs)
    maybeConfigs
      .map(
        _.split(",")
          .map(_.trim)
          .filter(_.nonEmpty)
          .toSeq
      )
      .getOrElse(configPaths)
  }

  override def getConfigs(path: Seq[String]): Seq[DmnConfig] =
    runtime.unsafeRun(
      loadConfigs(path)
    )

  private def loadConfigs(path: Seq[String]) = {
    for {
      zConfigs <- readConfigs(path.toList)
      dmnConfigs <- ZIO.collectAll(zConfigs)
      _ <- console.putStrLn(
        s"Found ${dmnConfigs.size} DmnConfigs in ${pwd / path}"
      )
    } yield dmnConfigs
  }

  override def addConfig(
      dmnConfig: DmnConfig,
      path: Seq[String]
  ): Seq[DmnConfig] =
    runtime.unsafeRun(
      DmnConfigHandler.write(dmnConfig, path.toList) *>
        loadConfigs(path)
    )

  override def updateConfig(dmnConfig: DmnConfig, path: Seq[String]): Seq[DmnConfig] =
    runtime.unsafeRun(
      DmnConfigHandler.delete(dmnConfig, path.toList) *>
        DmnConfigHandler.write(dmnConfig, path.toList) *>
        loadConfigs(path)
    )

  override def deleteConfig(dmnConfig: DmnConfig, path: Seq[String]): Seq[DmnConfig] =
    runtime.unsafeRun(
      DmnConfigHandler.delete(dmnConfig, path.toList) *>
        loadConfigs(path)
    )

  override def runTests(
      dmnConfigs: Seq[DmnConfig]
  ): Seq[Either[EvalException, DmnEvalResult]] =
    runtime.unsafeRun(for {
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
    } yield results)

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
          ZIO(
            getConfigFiles(file).map(f =>
              DmnConfigHandler.read(ops.Path(f).toIO)
            )
          ).mapError { ex =>
            ex.printStackTrace()
            ConfigException(ex.getMessage)
          }
      }
  }

  private def getConfigFiles(f: File): Seq[File] = {
      f.listFiles.filter(_.getName.endsWith(".conf"))
  }
}
