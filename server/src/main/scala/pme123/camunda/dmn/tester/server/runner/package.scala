package pme123.camunda.dmn.tester.server

import org.camunda.dmn.DmnEngine
import org.camunda.feel.syntaxtree.Val
import pme123.camunda.dmn.tester.shared.{Dmn, HandledTesterException}
import pme123.camunda.dmn.tester.shared.HandledTesterException.{ConfigException, ConsoleException, EvalException}
import zio.Console.printLine
import zio.IO

import java.io.IOException
import scala.language.implicitConversions

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

  def osPath(path: List[String]): os.Path =
    path.filterNot(_.isBlank) match {
      case Nil                        => os.pwd
      case x :: Nil if x.trim.isEmpty => os.pwd
      case ".." :: tail               => os.pwd / os.up / tail
      case other                      => os.pwd / other
    }

  def printError(msg: String): IO[ConsoleException, Unit] =
    print(
      scala.Console.RED + msg + scala.Console.RESET
    )
  def printWarning(msg: String): IO[ConsoleException, Unit] =
    print(
      scala.Console.YELLOW + msg + scala.Console.RESET
    )

  def print(msg: String): IO[ConsoleException, Unit] = {
    printLine(msg).mapError(exc => ConsoleException(s"ERROR: Problem with printing to console: ${exc.getMessage}"))
  }

}
