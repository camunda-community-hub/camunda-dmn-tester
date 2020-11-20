package pme123.camunda.dmn.tester

import java.io.File
import java.nio.file.{Path, Paths}

import ammonite.ops
import ammonite.ops.pwd
import org.camunda.dmn.DmnEngine
import zio._

import scala.language.implicitConversions

object TestRunner extends zio.App {

  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] =
    (for {
      _ <- console.putStrLn("Let's Start")
      config <- RunnerConfig.config
      dmnConfigs <- ZIO(readConfigs((pwd / config.basePath).toIO))
      auditLogRef <- Ref.make(Seq.empty[EvalResult])
      auditLogger <- UIO(AuditLogger(auditLogRef))
      engine <- UIO(new DmnEngine(auditLogListeners = List(auditLogger)))
      _ <- ZIO.foreach(dmnConfigs) {
        case DmnConfig(decisionId, data, dmnPath) =>
          ZIO(DmnTester(decisionId, dmnPath, engine).run(data))
      }
      audits <- auditLogRef.get
      _ <- ZIO.foreach(audits) { audit =>
        console.putStrLn(s" - $audit")
      }
    } yield ()).exitCode

  private def readConfigs(file: File) =
    getRecursively(file).map(f => DmnConfigHandler.read(ops.Path(f).toNIO))

  private def getRecursively(f: File): Seq[File] = {
    f.listFiles
      .filter(_.isDirectory)
      .flatMap(getRecursively) ++
      f.listFiles.filter(_.getName.endsWith(".json"))
  }
}
