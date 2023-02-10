package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.shared.HandledTesterException.ConsoleException
import zio.Console.printLine
import zio.IO

import scala.language.implicitConversions

package object runner {

  val defaultConfigPaths: Seq[String] = Seq(
    "/server/src/test/resources/dmn-configs"
  )
  val feelParseErrMsg = "FEEL expression: failed to parse expression"
  def feelParseErrHelp(message: String): String =
    s"""|$message
        |Hints:
        |> Read the message carefully - '' means you forgot to set a value.
        |> All outputs need a value.
        |> All Input-/ Output-Columns need an expression.
        |> Did you miss to wrap Strings in " - e.g. "TEXT"?
        |> Check if there is an 'empty' Rule you accidentally created.
        |> Check if all Values are valid FEEL expressions - see https://camunda.github.io/feel-scala/1.12/""".stripMargin

  def osPath(path: List[String]): os.Path =
    path.filterNot(_.isBlank) match {
      case Nil                        => os.pwd
      case x :: Nil if x.trim.isEmpty => os.pwd
      case ".." :: tail               => os.pwd / os.up / tail
      case other                      => os.pwd / other
    }

  def print(msg: String): IO[ConsoleException, Unit] = {
    printLine(msg).mapError(exc => ConsoleException(s"ERROR: Problem with printing to console: ${exc.getMessage}"))
  }

}
