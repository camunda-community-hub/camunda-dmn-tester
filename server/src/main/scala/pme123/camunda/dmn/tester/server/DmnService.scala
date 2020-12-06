package pme123.camunda.dmn.tester.server

import ammonite.ops
import ammonite.ops.pwd
import pme123.camunda.dmn.tester.server.zzz.{DmnConfigHandler, osPath}
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig}
import zio.{Runtime, ZIO, console}

import java.io.File

class DmnService extends DmnApi {
  private val runtime = Runtime.default

  override def getConfigs(path: Seq[String]): Seq[DmnConfig] = {
    runtime.unsafeRun(
      for {
        zConfigs <- readConfigs(path.toList)
        dmnConfigs <- ZIO.collectAll(zConfigs)
        _ <- console.putStrLn(
          s"Found ${dmnConfigs.size} DmnConfigs in ${pwd / path}"
        )
      } yield dmnConfigs
    )
  }

  private def readConfigs(path: List[String]) = {
    ZIO(osPath(path).toIO)
      .tap(f => console.putStrLn(s"Config Path: ${f.getAbsolutePath}"))
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

}
