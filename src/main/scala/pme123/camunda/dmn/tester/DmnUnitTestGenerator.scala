package pme123.camunda.dmn.tester

import ammonite.ops.{Callable1Implicit, Path, pwd, rm, write}
import org.camunda.dmn.DmnEngine

case class DmnUnitTestGenerator(decisionId: String, dmnPath: Seq[String]) {

  val dmnName: String = dmnPath.last
  val generatePath = Seq("target", "generated-tests")

  def generate(
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
