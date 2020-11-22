package pme123.camunda.dmn.tester

import java.io.InputStream

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.parser._
import os.read.inputStream
import pme123.camunda.dmn.tester
import zio.{IO, UIO, ZIO}

import scala.language.implicitConversions


case class DmnTester(
    decisionId: String,
    dmnPath: List[String],
    engine: DmnEngine = new DmnEngine()
) {

  def run(
      data: TesterData
  ): Either[DmnEngine.Failure, Seq[RunResult]] =
    parsedDmn().map(run(data, _))

  def run(
      data: TesterData,
      dmn: ParsedDmn
  ): Seq[RunResult] = {
    val allInputs: Seq[Map[String, Any]] = data.normalize()
    val evaluated =
      allInputs.map(inputMap =>
        RunResult(
          inputMap,
          engine.eval(dmn, decisionId, inputMap)
        )
      )
    evaluated
  }

  def parsedDmn(): Either[DmnEngine.Failure, ParsedDmn] = {
    parsedDmn(inputStream(osPath(dmnPath)))
  }

  def parsedDmn(
      streamToTest: InputStream
  ): Either[DmnEngine.Failure, ParsedDmn] = {
    engine.parse(streamToTest) match {
      case r @ Left(failure) =>
        println(
          s"FAILURE in ${dmnPath.mkString("/")} - $decisionId: $failure"
        )
        r
      case r => r
    }
  }

  def runDmnTest(
      inputs: Map[String, Any],
      expected: tester.EvalResult
  ): IO[String, String] =
    ZIO
      .fromEither(parsedDmn())
      .mapError(f => f.message)
      .flatMap(p => runDmnTest(p, inputs, expected))

  def runDmnTest(
      dmn: ParsedDmn,
      inputs: Map[String, Any],
      expected: tester.EvalResult
  ): IO[String, String] = {
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
        ZIO.fail(s"The expected ($expected) was not equal to $result")
      case Left(DmnEngine.Failure(message))
          if expected.failed.exists((e: EvalError) => e.msg == message) =>
        UIO(s"Success $message")
      case Left(DmnEngine.Failure(message)) =>
        ZIO.fail(s"Test failed: $message")
    }
  }
}
