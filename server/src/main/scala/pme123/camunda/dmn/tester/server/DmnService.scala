package pme123.camunda.dmn.tester.server

import ammonite.ops
import ammonite.ops.pwd
import org.camunda.dmn.DmnEngine
import pme123.camunda.dmn.tester.server.zzz._
import pme123.camunda.dmn.tester.shared.HandledTesterException.{
  ConfigException,
  EvalException
}
import pme123.camunda.dmn.tester.shared.{
  DmnApi,
  DmnConfig,
  DmnEvalResult,
  EvalResult
}
import zio.{Ref, Runtime, UIO, ZIO, console}

import java.io.File

class DmnService extends DmnApi {
  private val runtime = Runtime.default

  override def getBasePath(): String =
    pwd.toIO.getAbsolutePath

  override def getConfigs(path: Seq[String]): Seq[DmnConfig] =
    runtime.unsafeRun(
      for {
        zConfigs <- readConfigs(path.toList)
        dmnConfigs <- ZIO.collectAll(zConfigs)
        _ <- console.putStrLn(
          s"Found ${dmnConfigs.size} DmnConfigs in ${pwd / path}"
        )
      } yield dmnConfigs
    )

  override def runTests(
      dmnConfigs: Seq[DmnConfig]
  ): Seq[Either[EvalException, DmnEvalResult]] =
    runtime.unsafeRun(for {
      _ <- console.putStrLn("Let's start")
      auditLogRef <- Ref.make(Seq.empty[EvalResult])
      auditLogger <- UIO(AuditLogger(auditLogRef))
      engine <- UIO(new DmnEngine(auditLogListeners = List(auditLogger)))
      results <- ZIO.foreach(
        dmnConfigs
      )(dmnConfig =>
        DmnTester
          .testDmnTable(dmnConfig, engine)
          .flatMap(auditLogger.getDmnEvalResults)
          .map(Right.apply)
          .catchAll(ex => ZIO.left(ex))
      )
    } yield results)

  /*
    override def updateConfig(item: DmnConfig): Seq[DmnConfig] =
      runtime.unsafeRun(
        for {
          zConfigs <- readConfigs(path.toList)
          dmnConfigs <- ZIO.collectAll(zConfigs)
          _ <- console.putStrLn(
            s"Found ${dmnConfigs.size} DmnConfigs in ${pwd / path}"
          )
        } yield dmnConfigs
      )
   */

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
            getRecursively(file).map(f =>
              DmnConfigHandler.read(ops.Path(f).toIO)
            )
          ).mapError { ex =>
            ex.printStackTrace()
            ConfigException(ex.getMessage)
          }
      }
  }

  private def getRecursively(f: File): Seq[File] = {
    f.listFiles
      .filter(_.isDirectory)
      .flatMap(getRecursively) ++
      f.listFiles.filter(_.getName.endsWith(".conf"))
  }
}
