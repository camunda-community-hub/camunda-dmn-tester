package pme123.camunda.dmn.tester

import java.io.File

import ammonite.ops
import ammonite.ops.pwd
import org.camunda.dmn.DmnEngine
import zio._
import zio.console.Console

import scala.language.implicitConversions

object TestRunner extends zio.App {

  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] =
    RunnerConfig.config.flatMap(runApp).exitCode

  def runApp(config: RunnerConfig): ZIO[Console, Serializable, Unit] = {
    for {
      config <- RunnerConfig.config
      dmnConfigs <- ZIO.collectAll(readConfigs((pwd / config.basePath).toIO))
      auditLogRef <- Ref.make(Seq.empty[EvalResult])
      auditLogger <- UIO(AuditLogger(auditLogRef))
      engine <- UIO(new DmnEngine(auditLogListeners = List(auditLogger)))
      _ <- ZIO.foreach_(dmnConfigs) {
        case DmnConfig(decisionId, data, dmnPath) =>
          ZIO.fromEither(DmnTester(decisionId, dmnPath, engine).run(data))
      }
      _ <- auditLogger.printLog()
    } yield ()
  }

  private def readConfigs(file: File) =
    getRecursively(file).map(f => DmnConfigHandler.read(ops.Path(f).toIO))

  private def getRecursively(f: File): Seq[File] = {
    f.listFiles
      .filter(_.isDirectory)
      .flatMap(getRecursively) ++
      f.listFiles.filter(_.getName.endsWith(".conf"))
  }
}
