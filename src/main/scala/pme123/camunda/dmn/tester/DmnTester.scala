package pme123.camunda.dmn.tester

import java.io.InputStream

import ammonite.ops._
import org.camunda.dmn.Audit.{AuditLogListener, DecisionTableEvaluationResult}
import org.camunda.dmn.DmnEngine._
import org.camunda.dmn.parser._
import org.camunda.dmn.{Audit, DmnEngine}
import org.camunda.feel._
import org.camunda.feel.context.Context
import org.camunda.feel.syntaxtree._
import org.junit.Assert.{assertEquals, fail}
import os.read.inputStream

case class DmnTester(decisionId: String, dmnPath: Seq[String]) {
  private val engine = new DmnEngine(
    auditLogListeners = List(new AuditLogListener {
      override def onEval(log: Audit.AuditLog): Unit = {
        println(s"AUDITLOG:")
        log.rootEntry.result match {
          case DecisionTableEvaluationResult(inputs, matchedRules, result) =>
            println(
              "- Inputs: " + inputs
                .map(i => s"${i.input.name}: ${unwrap(i.value)}")
                .mkString(", ")
            )
            println(
              "- Matched Rules: " + matchedRules
                .map(rule =>
                  s"  - Id:      ${rule.rule.id}" +
                    "\n  - Outputs: " + rule.outputs
                      .map(out => s"${out.output.name}: ${unwrap(out.value)}")
                      .mkString(", ")
                )
                .mkString("\n", "\n", "")
            )
          case result: Audit.EvaluationResult =>
            println(s"- Result: ${result.result}")
        }

      }
    })
  )

  val generatePath = Seq("target", "generated-tests")
  val dmnName = dmnPath.last

  case class RunResult(
      inputs: Map[String, Any],
      result: Either[Failure, EvalResult]
  )

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

  def parsedDmn(): Either[Failure, ParsedDmn] = {
    parsedDmn(inputStream(pwd / dmnPath))
  }

  def parsedDmn(
      streamToTest: InputStream
  ): Either[Failure, ParsedDmn] = {
    engine.parse(streamToTest) match {
      case r as Left(failure) =>
        println(
          s"FAILURE in ${dmnPath.mkString("/")} - ${decisionId}: $failure"
        )
        r
      case r => r
    }
  }

  def runDmnTest(
      inputs: Map[String, Any],
      expected: EvalResult
  ): Unit =
    parsedDmn()
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
          case RunResult(_, Right(Result(rMap: Map[_, _]))) =>
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
      case RunResult(inputMap, Right(NilResult)) =>
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

    private def unwrap(value: Val): String =
        engine.valueMapper.unpackVal(value) match {
          case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
          case Some(value)    => value.toString
          case None           => "NO VALUE"
          case value => 
            value.toString
        }
}
