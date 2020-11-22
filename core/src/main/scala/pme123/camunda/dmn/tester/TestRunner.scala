package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops
import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.Failure
import zio._
import zio.console.Console

import scala.language.implicitConversions

object TestRunner extends zio.App {

  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] =
    RunnerConfig.config.flatMap(runApp).exitCode

  def runApp(config: RunnerConfig): ZIO[Console, Serializable, Unit] = {
    for {
      _ <- console.putStrLn(s"Using: $config")
      dmnConfigs <- ZIO.collectAll(readConfigs(config.basePath))
      auditLogRef <- Ref.make(Seq.empty[EvalResult])
      auditLogger <- UIO(AuditLogger(auditLogRef))
      engine <- UIO(new DmnEngine(auditLogListeners = List(auditLogger)))
      _ <- ZIO.foreach_(dmnConfigs)(testDmnTable(_, engine))
      _ <- auditLogger.printLog()
    } yield ()
  }

  private def readConfigs(path: List[String]) =
    getRecursively(osPath(path).toIO).map(f =>
      DmnConfigHandler.read(ops.Path(f).toIO)
    )

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
    ) *> ZIO
      .fromEither(DmnTester(decisionId, dmnPath, engine).run(data))
      .catchAll {
        case Failure(message)
            if message.contains("Failed to parse FEEL expression ''") =>
          printError(
            s"""|ERROR: Could not parse a FEEL expression in the DMN table: $decisionId.
                    |> Hint: All outputs need a value.""".stripMargin
          )
        case other =>
          ZIO(other)
      }
  }
}
