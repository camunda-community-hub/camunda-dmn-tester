package pme123.camunda.dmn.tester.server

import ammonite.ops.{pwd, up}
import org.camunda.dmn.DmnEngine
import org.camunda.feel.syntaxtree.Val
import os.Path
import pme123.camunda.dmn.tester.shared.Dmn
import zio.console.Console
import zio.{URIO, console}

package object runner {

  val defaultConfigPaths: Seq[String] = Seq(
    "/server/src/test/resources/dmn-configs"
  )

  case class RunResults(dmn: Dmn, results: Seq[RunResult])

  case class RunResult(
      inputs: Map[String, Any],
      result: Either[DmnEngine.Failure, Val]
  )

  def formatStrings(strings: Seq[String]): String = {
    val inputFormatter = "%1$23s"
    strings
      .map {
        case i if i.length > 22 => inputFormatter.format(i.take(20) + "..")
        case i                  => inputFormatter.format(i.take(22))

      }
      .mkString("| ", " | ", " |")
  }

  def osPath(path: List[String]): Path =
    path.filterNot(_.isBlank) match {
      case Nil                        => pwd
      case x :: Nil if x.trim.isEmpty => pwd
      case ".." :: tail               => pwd / up / tail
      case other                      => pwd / other
    }

  def printError(msg: String): URIO[Console, Unit] =
    console.putStrLn(
      scala.Console.RED + msg + scala.Console.RESET
    )
  def printWarning(msg: String): URIO[Console, Unit] =
    console.putStrLn(
      scala.Console.YELLOW + msg + scala.Console.RESET
    )
}
