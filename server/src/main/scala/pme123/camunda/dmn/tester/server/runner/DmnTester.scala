package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.Failure
import org.camunda.dmn.parser._
import os.read.inputStream
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.{IO, ZIO}

import java.io.InputStream
import scala.language.implicitConversions
import scala.util.Try

case class DmnTester(
    dmnConfig: DmnConfig,
    engine: DmnEngine = new DmnEngine()
) {
  val DmnConfig(decisionId, data, dmnPath, _, _) = dmnConfig

  def run(): IO[EvalException, DmnEvalResult] =
    parsedDmn().flatMap(run)

  def run(
      dmn: ParsedDmn
  ): IO[EvalException, DmnEvalResult] = {
    val allInputs: Seq[Map[String, Any]] = data.allInputs()

    val engine = DmnTableEngine(dmn, dmnConfig)
    for {
      decision <- engine.evalDecision(dmnConfig.data.inputKeys, allInputs)
      // _ = engine.checkTestCases()
    } yield decision
  }

  def parsedDmn(): IO[EvalException, ParsedDmn] =
    for {
      is <- ZIO
        .fromTry(Try(inputStream(osPath(dmnPath))))
        .orElseFail(
          EvalException(
            dmnConfig,
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
            if message.contains("FEEL expression: failed to parse expression") =>
          EvalException(
            dmnConfig,
            s"""|$message
                |Hints:
                |> Read the message carefully - '' means you forgot to set a value.
                |> All outputs need a value.
                |> All Input-/ Output-Columns need an expression.
                |> Did you miss to wrap Strings in " - e.g. "TEXT"?
                |> Check if there is an 'empty' Rule you accidentally created.
                |> Check if all Values are valid FEEL expressions - see https://camunda.github.io/feel-scala/1.12/""".stripMargin
          )
        case Failure(msg) =>
          EvalException(dmnConfig, msg)
      }
  }

}

object DmnTester {

  def testDmnTable(
      dmnConfig: DmnConfig,
      engine: DmnEngine
  ): IO[HandledTesterException, Either[EvalException, DmnEvalResult]] = {
    val DmnConfig(decisionId, _, dmnPath, _, testUnit) = dmnConfig
    print(
      s"Start testing $decisionId (testUnit = $testUnit): $dmnPath (${osPath(dmnPath)})"
    ) *>
      DmnTester(dmnConfig, engine)
        .run()
        .map(Right.apply)
        .catchAll(ex => ZIO.left(ex))
  }
}
