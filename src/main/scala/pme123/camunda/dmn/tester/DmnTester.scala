package pme123.camunda.dmn.tester

import java.io.InputStream
import java.math.BigDecimal

import ammonite.ops._
import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine._
import org.camunda.dmn.parser._
import org.junit.Assert.{assertEquals, fail}
import os.read.inputStream

import scala.language.implicitConversions
import scala.util.Random

case class TesterData(
    inputs: List[TesterInput]
) {

  lazy val inputKeys: Seq[String] = inputs.map { case TesterInput(k, _) => k }

  def normalize(): List[Map[String, Any]] = {
    val data = inputs.map(_.normalize())
    cartesianProduct(data).map(_.toMap)
  }

  def cartesianProduct(
      xss: List[(String, List[Any])]
  ): List[List[(String, Any)]] =
    xss match {
      case Nil => List(Nil)
      case (key, v) :: t =>
        for (xh <- v; xt <- cartesianProduct(t)) yield (key -> xh) :: xt
    }
}

case class TesterInput(key: String, values: List[TesterValue]) {
  def normalize(): (String, List[Any]) = {
    val allValues: List[Any] = values.flatMap(_.normalized)
    key -> allValues
  }
}

enum TesterValue(val normalized: Set[Any]) {

  case  StringValue(value: String) extends TesterValue(Set(value))

  case BooleanValue(value: Boolean) extends TesterValue( Set(value))

  case  NumberValue(value: Number) extends TesterValue(Set(value))

  case  ValueSet(values: Set[TesterValue]) extends TesterValue(values.flatMap(_.normalized))

  case RandomInts(count: Int) extends TesterValue  (List.fill(count)(Random.nextInt()).toSet)
  
}

object implicits {

  implicit def string2Value(x: String): TesterValue =
    TesterValue.StringValue(x)

  implicit def number2Value(x: Int): TesterValue =
    TesterValue.NumberValue(new BigDecimal(x))

  implicit def boolean2Value(x: Boolean): TesterValue =
    TesterValue.BooleanValue(x)
}

case class DmnTester(dmnName: String, decisionId: String) {
  private val engine = new DmnEngine()
  val testPath = Seq("src", "test", "resources")
  val mainPath = Seq("src", "main", "resources")
  val generatePath = Seq("target", "generated-tests")

  def run(
      data: TesterData,
      dmn: ParsedDmn
  ): Seq[(Map[String, Any], Either[Failure, EvalResult])] = {
    val allInputs: Seq[Map[String, Any]] = data.normalize()
    val evaluated: Seq[(Map[String, Any], Either[Failure, EvalResult])] =
      allInputs.map(inputMap =>
        inputMap -> engine
          .eval(dmn, decisionId, inputMap)
      )
    evaluated
  }

  def parsedDmn(
      path: Seq[String] = mainPath
  ): Either[Failure, ParsedDmn] = {
    parsedDmn(inputStream(pwd/path/s"$dmnName.dmn"))
  }

  def parsedDmn(
      streamToTest: InputStream
  ): Either[Failure, ParsedDmn] = {
    engine.parse(streamToTest)  match {
      case r@Left(failure) =>
        println(s"FAILURE in $dmnName - $decisionId: $failure")
        r
      case r => r
    }
  }

  def runDmnTest(
      path: Seq[String],
      inputs: Map[String, Any],
      expected: EvalResult
  ): Unit =
    parsedDmn(path)
      .map(runDmnTest(_, inputs, expected))

  def runDmnTest(
      dmn: ParsedDmn,
      inputs: Map[String, Any],
      expected: EvalResult
  ): Unit = {

    val result = engine.eval(dmn, decisionId, inputs)

    result match {
      case Left(failure) => fail(s"Failure $failure")
      case Right(r) =>
        assertEquals(s"Success $r", expected, r)
    }
  }

  def printTestResult(
      name: String,
      inputs: Seq[String],
      evaluated: Seq[(Map[String, Any], Either[Failure, EvalResult])]
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
          case (_, Right(Result(rMap: Map[_, _]))) =>
            rMap.keySet.map(_.toString)
          case _ => Nil
        }
        .filter(_.nonEmpty)
        .take(1)
        .headOption
        .getOrElse(Nil)
        .toSeq
    }
    def formatResult(evalResult: EvalResult) =
      evalResult match {
        case Result(rMap: Map[_, _]) =>
          formatStrings(rMap.map { case (_, v) => v.toString }.toSeq)
        case Result(other) => other.toString
        case NilResult     => "NO RESULT"
      }

    println(s"*" * 100)
    println(name)
    println(
      s"EVALUATED: ${formatStrings(inputs)} -> ${formatStrings(extractOutputs())}"
    )
    println(s"*" * 100)
    evaluated.foreach {
      case (inputMap, Right(result)) =>
        println(
          s"INFO:      ${formatStrings(inputs.map(k => inputMap(k).toString))} -> ${formatResult(result)}"
        )
      case (inputMap, Left(failure)) =>
        println(
          scala.Console.RED + s"ERROR:     ${formatStrings(
            inputs.map(k => inputMap(k).toString)
          )} -> $failure" + scala.Console.RESET
        )
    }
  }

  def generateDmnTests(
      inputs: Seq[String],
      evaluated: Seq[(Map[String, Any], Either[Failure, EvalResult])],
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
        case (inputs, Right(evalResult)) =>
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
              case Result(result) => s"Result(${outputAsString(result)})"
              case NilResult      => "NilResult"
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
