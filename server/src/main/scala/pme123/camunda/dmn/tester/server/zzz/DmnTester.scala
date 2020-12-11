package pme123.camunda.dmn.tester.server.zzz

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.Failure
import org.camunda.dmn.parser._
import os.read.inputStream
import pme123.camunda.dmn.tester.server.HandledTesterException
import pme123.camunda.dmn.tester.shared.{
  DmnConfig,
  EvalError,
  EvalResult,
  TesterData
}
import zio.console.Console
import zio.{IO, UIO, ZIO, console}

import java.io.InputStream
import scala.language.implicitConversions

case class DmnTester(
    decisionId: String,
    dmnPath: List[String],
    engine: DmnEngine = new DmnEngine()
) {

  def run(data: TesterData): ZIO[Any, HandledTesterException, RunResults] =
    parsedDmn().map(run(data, _))

  def run(
      data: TesterData,
      dmn: ParsedDmn
  ): RunResults = {
    val allInputs: Seq[Map[String, Any]] = data.normalize()
    val evaluated = {
      allInputs.map(inputMap =>
        RunResult(
          inputMap,
          engine.eval(dmn, decisionId, inputMap)
        )
      )
    }
    val rulIds = dmn.decisions
      .find(_.id == decisionId)
      .toSeq
      .map(_.logic)
      .collect { case ParsedDecisionTable(_, _, rules, _, _) =>
        rules.map(_.id)
      }
      .flatten

    RunResults(Dmn(decisionId, rulIds), evaluated)
  }

  def parsedDmn(): IO[HandledTesterException, ParsedDmn] =
    for {
      is <- ZIO(inputStream(osPath(dmnPath)))
        .orElseFail(
          HandledTesterException(
            s"There was no DMN in ${dmnPath.mkString("/")}."
          )
        )
      dmn <- parsedDmn(is)
    } yield dmn

  def parsedDmn(
      streamToTest: InputStream
  ): IO[HandledTesterException, ParsedDmn] = {
    ZIO
      .fromEither(engine.parse(streamToTest))
      .mapError {
        case Failure(message)
            if message.contains("Failed to parse FEEL expression ''") =>
          HandledTesterException(
            s"""|ERROR: Could not parse a FEEL expression in the DMN table: $decisionId.
              |Hints:
              |> All outputs need a value.
              |> Did you miss to wrap Strings in " - e.g. "TEXT"?
              |> Check if all Values are valid FEEL expressions - see https://camunda.github.io/feel-scala/1.12/""".stripMargin
          )
        case Failure(msg) =>
          HandledTesterException(msg)
      }
  }

  def runDmnTest(
      inputs: Map[String, Any],
      expected: EvalResult
  ): IO[HandledTesterException, String] =
    parsedDmn()
      .flatMap(p => runDmnTest(p, inputs, expected))

  def runDmnTest(
      dmn: ParsedDmn,
      inputs: Map[String, Any],
      expected: EvalResult
  ): IO[HandledTesterException, String] = {
    engine.eval(dmn, decisionId, inputs) match {
      case Right(DmnEngine.NilResult) if expected.matchedRules.isEmpty =>
        UIO(s"Success No Result")
      case Right(DmnEngine.Result(value: Map[_, _]))
          if expected.matchedRules.flatMap(_.outputs).toMap == value.map {
            case (k, v) => s"$k" -> s"$v"
          } =>
        UIO(s"Success $value")
      case Right(DmnEngine.Result(value: Any))
          if expected.matchedRules.head.outputs.head._2 == s"$value" =>
        UIO(s"Success $value")
      case Right(result) =>
        ZIO.fail(
          HandledTesterException(
            s"The expected ($expected) was not equal to $result"
          )
        )
      case Left(DmnEngine.Failure(message))
          if expected.failed.exists((e: EvalError) => e.msg == message) =>
        UIO(s"Success $message")
      case Left(DmnEngine.Failure(message)) =>
        ZIO.fail(HandledTesterException(s"Test failed: $message"))
    }
  }
}

object DmnTester {
  def testDmnTable(
      dmnConfig: DmnConfig,
      engine: DmnEngine
  ): ZIO[Console, Nothing, Option[RunResults]] = {
    val DmnConfig(decisionId, data, dmnPath, _) = dmnConfig
    console.putStrLn(
      s"Start testing $decisionId: $dmnPath (${osPath(dmnPath)})"
    ) *>
      DmnTester(decisionId, dmnPath, engine)
        .run(data)
        .map(r => Some(r))
        .catchAll { case HandledTesterException(msg) =>
          printError(
            s"ERROR: $msg"
          ) *> UIO.none
        }
  }
}
