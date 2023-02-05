package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.shared.HandledTesterException.ConsoleException
import zio.Console.printLine
import zio.IO

import scala.language.implicitConversions

package object runner {

  val defaultConfigPaths: Seq[String] = Seq(
    "/server/src/test/resources/dmn-configs"
  )

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
