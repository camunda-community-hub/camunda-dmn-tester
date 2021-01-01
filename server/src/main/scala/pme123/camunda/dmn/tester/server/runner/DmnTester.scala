package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.Failure
import org.camunda.dmn.parser._
import os.read.inputStream
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.console.Console
import zio.{IO, UIO, ZIO, console}

import java.io.InputStream
import scala.language.implicitConversions

case class DmnTester(
    dmnConfig: DmnConfig,
    engine: DmnEngine = new DmnEngine()
) {
  val DmnConfig(decisionId, data, dmnPath, _) = dmnConfig

  def run(): ZIO[Any, EvalException, RunResults] =
    parsedDmn().map(run)

  def run(
      dmn: ParsedDmn
  ): RunResults = {
    val allInputs: Seq[Map[String, Any]] = data.normalize()
    val evaluated = {
      allInputs.map(inputMap => {
        RunResult(
          inputMap,
          engine.eval(dmn, decisionId, inputMap)
        )
      })
    }
    val maybeDecision = dmn.decisions
      .find(_.id == decisionId)
    val hitPolicyAndRules = maybeDecision
      .map(_.logic)
      .collect { case ParsedDecisionTable(_, _, rules, hitPolicy, _) =>
        hitPolicy -> rules.zipWithIndex
          .map { case (ParsedRule(id, inputs, outputs), index) =>
            DmnRule(
              index + 1,
              id,
              inputs.map(_.text).toSeq,
              outputs.map(_._2.text).toSeq
            )
          }
      }
    RunResults(
      Dmn(
        decisionId,
        hitPolicyAndRules
          .map { case (hitPolicy, _) => hitPolicy.toString }
          .getOrElse("NOT FOUND"),
        dmnConfig,
        hitPolicyAndRules.toSeq.flatMap { case (_, rules) => rules }
      ),
      evaluated
    )
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
              |> Check if all Values are valid FEEL expressions - see https://camunda.github.io/feel-scala/1.12/\n""".stripMargin
          )
        case Failure(msg) =>
          EvalException(decisionId, msg)
      }
  }

  def runDmnTest(
      inputs: Map[String, Any],
      expected: EvalResult
  ): IO[EvalException, String] =
    parsedDmn()
      .flatMap(p => runDmnTest(p, inputs, expected))

  def runDmnTest(
      dmn: ParsedDmn,
      inputs: Map[String, Any],
      expected: EvalResult
  ): IO[EvalException, String] = {
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
          EvalException(
            decisionId,
            s"The expected ($expected) was not equal to $result"
          )
        )
      case Left(DmnEngine.Failure(message))
          if expected.failed.exists((e: EvalError) => e.msg == message) =>
        UIO(s"Success $message")
      case Left(DmnEngine.Failure(message)) =>
        ZIO.fail(EvalException(decisionId, s"Test failed: $message"))
    }
  }
}

object DmnTester {
  def testDmnTable(
      dmnConfig: DmnConfig,
      engine: DmnEngine
  ): ZIO[Console, EvalException, RunResults] = {
    val DmnConfig(decisionId, _, dmnPath, _) = dmnConfig
    console.putStrLn(
      s"Start testing $decisionId: $dmnPath (${osPath(dmnPath)})"
    ) *>
      DmnTester(dmnConfig, engine)
        .run()
  }
}
