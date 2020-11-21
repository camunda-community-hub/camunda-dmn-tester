package pme123.camunda.dmn.tester

import java.io.InputStream

import ammonite.ops._
import org.camunda.dmn.DmnEngine
import org.camunda.dmn.parser._
import os.read.inputStream
import pme123.camunda.dmn.tester
import zio.{IO, UIO, ZIO}

import scala.language.implicitConversions

case class DmnTester(
    decisionId: String,
    dmnPath: Seq[String],
    engine: DmnEngine = new DmnEngine()
) {

  val generatePath = Seq("target", "generated-tests")
  val dmnName: String = dmnPath.last

  case class RunResult(
      inputs: Map[String, Any],
      result: Either[DmnEngine.Failure, DmnEngine.EvalResult]
  )

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
    parsedDmn(inputStream(pwd / dmnPath))
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

  def printTestResult(
      name: String,
      inputs: Seq[String],
      evaluated: Seq[RunResult]
  ): Unit = {

    def formatStrings(strings: Seq[String]) = {
      val inputFormatter = "%1$20s"
      strings
        .map(i => inputFormatter.format(i.take(20)))
        .mkString("| ", " | ", " |")
    }

    def extractOutputs() = {
      evaluated
        .map {
          case RunResult(_, Right(DmnEngine.Result(rMap: Map[_, _]))) =>
            rMap.keySet.map(_.toString)
          case _ => Nil
        }
        .filter(_.nonEmpty)
        .take(1)
        .headOption
        .getOrElse(Nil)
        .toSeq
    }
    def formatResult(evalResult: DmnEngine.EvalResult) =
      evalResult match {
        case DmnEngine.Result(rMap: Map[_, _]) =>
          formatStrings(rMap.map { case (_, v) => v.toString }.toSeq)
        case DmnEngine.Result(other) => other.toString
        case DmnEngine.NilResult     => "NO RESULT"
      }

    //  println(s"*" * 100)
    println(name)
    println(
      s"EVALUATED: ${formatStrings(inputs)} -> ${formatStrings(extractOutputs())}"
    )
//    println(s"*" * 100)
    def formatInputMap(inputMap: Map[String, Any]) = {
      formatStrings(inputs.map(k => inputMap(k).toString))
    }

    evaluated.foreach {
      case RunResult(inputMap, Right(DmnEngine.NilResult)) =>
        println(
          scala.Console.YELLOW +
            s"WARN:      ${formatInputMap(inputMap)} -> NO RESULT" +
            scala.Console.RESET
        )
      case RunResult(inputMap, Right(result)) =>
        println(
          s"INFO:      ${formatInputMap(inputMap)} -> ${formatResult(result)}"
        )
      case RunResult(inputMap, Left(failure)) =>
        println(
          scala.Console.RED +
            s"ERROR:     ${formatInputMap(inputMap)} -> $failure" +
            scala.Console.RESET
        )
    }
  }

  def generateDmnTests(
      inputs: Seq[String],
      evaluated: Seq[RunResult],
      packageName: String = "pme123.camunda.dmn"
  ): Unit = {
    val className =
      s"${dmnName.trim
        .split("[ -_]")
        .map(n => s"${n.head.toUpper}${n.tail}")
        .mkString}Test"
    val inputParams = inputs.map(i => s"""  val $i = "$i"""").mkString("\n")
    lazy val testMethods = evaluated
      .map {
        case RunResult(inputs, Right(evalResult)) =>
          testMethod(
            "test" + inputs.values
              .map(_.toString)
              .map(n =>
                s"${n.head.toUpper}${n.tail}".replaceAll("[ -]", "").take(10)
              )
              .mkString("_"),
            inputs.map {
              case (k, v: String) =>
                s""""$k"""" -> s""""$v""""
              case (k, v) =>
                s""""$k"""" -> v.toString
            }.toString,
            evalResult match {
              case DmnEngine.Result(result) =>
                s"Result(${outputAsString(result)})"
              case DmnEngine.NilResult => "NilResult"
            }
          )
        case other =>
          println(
            s"${scala.Console.YELLOW}No test method generated for: $other${scala.Console.RESET}"
          )
          ""
      }
      .filter(_.trim.nonEmpty)
      .mkString("\n")

    def outputAsString(result: Any): String =
      result match {
        case rMap: Map[_, _] =>
          s"${rMap.map {
            case (k, list: Seq[_]) =>
              s""""$k"""" -> list.map(outputAsString).mkString("\n")
            case (k, v: String) => s""""$k"""" -> s""""$v""""
            case (k, v)         => s""""$k"""" -> v
          }}"
        case list: Seq[_] =>
          list
            .map(outputAsString)
            .mkString("List(\n        ", ",\n        ", ")")
        case other: String => s""""$other""""
        case other         => s"""$other"""
      }

    def testMethod(testName: String, inputs: String, outputs: String): String =
      s"""  @Test
         |  def $testName(): Unit = {
         |    tester.runDmnTest(
         |      testPath,
         |      $inputs,
         |      $outputs
         |    )
         |  }
         |""".stripMargin
    lazy val genClass = s"""package $packageName
                      |
                      |import org.camunda.dmn.DmnEngine.{NilResult, Result}
                      |import org.junit.Test
                      |import pme123.camunda.dmn.tester._
                      |import scala.language.implicitConversions
                      |
                      |//noinspection TypeAnnotation
                      |class $className {
                      |
                      |$inputParams
                      |
                      |  val tester = DmnTester("$dmnName", "$decisionId")
                      |  val testPath = tester.testPath // path to where the DMN is located
                      |$testMethods
                      |}""".stripMargin

    println(genClass)

    val filePath: Path = pwd / generatePath / packageName.split(
      """\."""
    ) / s"$className.scala"
    rm ! filePath
    write(filePath, genClass, createFolders = true)
  }
}
