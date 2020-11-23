package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops
import org.camunda.dmn.DmnEngine
import pme123.camunda.dmn.HandledTesterException
import zio._
import zio.console.Console

import scala.language.implicitConversions

object TestRunner extends zio.App {

  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] =
    RunnerConfig.config.flatMap(runApp).exitCode

  def runApp(
      config: RunnerConfig
  ): ZIO[Console, Serializable, Seq[EvalResult]] = {
    for {
      _ <- console.putStrLn(s"Using: $config")
      zConfigs <- readConfigs(config.basePath)
      dmnConfigs <- ZIO.collectAll(zConfigs)
      auditLogRef <- Ref.make(Seq.empty[EvalResult])
      auditLogger <- UIO(AuditLogger(auditLogRef))
      engine <- UIO(new DmnEngine(auditLogListeners = List(auditLogger)))
      _ <- ZIO.foreach_(dmnConfigs)(testDmnTable(_, engine))
      _ <- auditLogger.printLog()
      result <- auditLogRef.get
    } yield result
  }

  private def readConfigs(path: List[String]) = {
    ZIO(osPath(path).toIO)
      .mapError { ex =>
        ex.printStackTrace()
        HandledTesterException(ex.getMessage)
      }
      .flatMap {
        case f if !f.exists() =>
          ZIO.fail(
            HandledTesterException(
              s"Your provided Config Path does not exist (${f.getAbsolutePath})."
            )
          )
        case f if !f.isDirectory =>
          ZIO.fail(
            HandledTesterException(
              s"Your provided Config Path is not a directory (${f.getAbsolutePath})."
            )
          )
        case file =>
          ZIO(
            getRecursively(file).map(f =>
              DmnConfigHandler.read(ops.Path(f).toIO)
            )
          ).mapError { ex =>
            ex.printStackTrace()
            HandledTesterException(ex.getMessage)
          }
      }
  }

  private def getRecursively(f: File): Seq[File] = {
    f.listFiles
      .filter(_.isDirectory)
      .flatMap(getRecursively) ++
      f.listFiles.filter(_.getName.endsWith(".conf"))
  }

  private def testDmnTable(dmnConfig: DmnConfig, engine: DmnEngine) = {
    val DmnConfig(decisionId, data, dmnPath) = dmnConfig
    console.putStrLn(
      s"Start testing $decisionId: $dmnPath (${osPath(dmnPath)})"
    ) *>
      DmnTester(decisionId, dmnPath, engine)
        .run(data)
        .catchAll { case HandledTesterException(msg) =>
             printError(
              s"ERROR: $msg"
            )
        }

  }
}
