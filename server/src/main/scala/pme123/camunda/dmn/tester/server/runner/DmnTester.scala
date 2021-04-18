package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.Failure
import org.camunda.dmn.parser._
import os.read.inputStream
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.console.Console
import zio.{IO, ZIO, console}

import java.io.InputStream
import scala.language.implicitConversions

case class DmnTester(
                      dmnConfig: DmnConfig,
                      engine: DmnEngine = new DmnEngine()
                    ) {
  val DmnConfig(decisionId, data, dmnPath, _) = dmnConfig

  def run(): ZIO[Any, EvalException, DmnEvalResult] =
    parsedDmn().flatMap(run)

  def run(
           dmn: ParsedDmn
         ): ZIO[Any, EvalException, DmnEvalResult] = {
    val allInputs: Seq[Map[String, Any]] = data.allInputs()
    val engine = DmnTableEngine(dmn, dmnConfig)
    for {
      decision <- engine.evalDecision(allInputs)
      // _ = engine.checkTestCases()
    } yield decision
  }

  def parsedDmn(): IO[EvalException, ParsedDmn] =
    for {
      is <- ZIO(inputStream(osPath(dmnPath)))
        .orElseFail(
          EvalException(
            decisionId,
            s"There was no DMN in ${dmnPath.mkString("/")}."
          )
        )
      dmn <- parsedDmn(is)
    } yield dmn

  def parsedDmn(
                 streamToTest: InputStream
               ): IO[EvalException, ParsedDmn] = {
    ZIO
      .fromEither(engine.parse(streamToTest))
      .mapError {
        case Failure(message)
          if message.contains("Failed to parse FEEL expression ''") =>
          EvalException(
            decisionId,
            s"""|ERROR: Could not parse a FEEL expression in the DMN table: $decisionId.\n
                |Hints:\n
                |> All outputs need a value.\n
                |> Did you miss to wrap Strings in " - e.g. "TEXT"?\n
                |> Check if there is an 'empty' Rule you accidently created.\n
                |> Check if all Values are valid FEEL expressions - see https://camunda.github.io/feel-scala/1.12/\n""".stripMargin
          )
        case Failure(msg) =>
          EvalException(decisionId, msg)
      }
  }

}

object DmnTester {

  def testDmnTable(
                    dmnConfig: DmnConfig,
                    engine: DmnEngine
                  ): ZIO[Console, EvalException, DmnEvalResult] = {
    val DmnConfig(decisionId, _, dmnPath, _) = dmnConfig
    console.putStrLn(
      s"Start testing $decisionId: $dmnPath (${osPath(dmnPath)})"
    ) *>
      DmnTester(dmnConfig, engine)
        .run()
  }
}
