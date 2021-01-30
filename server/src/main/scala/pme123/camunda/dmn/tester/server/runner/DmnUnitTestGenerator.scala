package pme123.camunda.dmn.tester.server.runner

import ammonite.ops.{Callable1Implicit, rm}
import os.write
import pme123.camunda.dmn.tester.server.{ZDmnService => z}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.console.Console
import zio.{Runtime, UIO, ZIO}

case class UnitTestGeneratorConfig(
    packageName: String = "pme123.camunda.dmn.tester.test",
    outputPath: List[String] = List("server", "target", "generated-tests")
)
case class DmnUnitTestGenerator(
    config: UnitTestGeneratorConfig = UnitTestGeneratorConfig()
) {
  private val runtime = Runtime.default

  def run(): Unit = runtime.unsafeRun(generate())

  def generate(): ZIO[Console, Any, Unit] =
    for {
      configPaths <- z.loadConfigPaths()
      configs <- ZIO.foreach(configPaths)(p =>
        z.loadConfigs(p.split("/").filter(_.trim.nonEmpty))
      )
      testResults <- z.runTests(configs.flatten)
      _ <- ZIO.foreach_(testResults)(r => generate(r))
    } yield ()

  def generate(
      testResult: Either[EvalException, DmnEvalResult]
  ): ZIO[Any, _, _] =
    testResult match {
      case Left(exc) =>
        for {
          name <- className(exc.decisionId)
          testMethod <- testMethod(exc)
          _ <- testFile(name, testMethod)
        } yield ()
      case Right(result) =>
        for {
          name <- className(result.dmn.id)
          testMethod <- testMethods(result)
          _ <- testFile(name, testMethod: _*)
        } yield ()
    }

  private[runner] def className(decisionId: String) =
    UIO(s"${decisionId.trim
      .split("""[\W]""")
      .filter(_.trim.nonEmpty)
      .map(n => n.capitalize)
      .mkString}Test")

  private[runner] def testMethods(dmnEvalResult: DmnEvalResult) = {
    ZIO.foreach(dmnEvalResult.evalResults) { dmnEvalRow =>
      testMethod(dmnEvalRow, dmnEvalResult)
    }
  }

  private[runner] def info(
      dmnEvalRow: DmnEvalRowResult,
      dmnEvalResult: DmnEvalResult
  ): UIO[String] =
    UIO {
      val DmnEvalResult(dmn, inputKeys, outputKeys, evalResults, missingRules) =
        dmnEvalResult
      val DmnEvalRowResult(
        status,
        decisionId,
        testInputs,
        matchedRules,
        maybeError
      ) = dmnEvalRow
      val inset: String = "   "
      def matchRule(rule: MatchedRule): String = {
        val MatchedRule(ruleId, inputs, outputs) = rule
        s"""- Matched Rule: $ruleId
           |$inset- Inputs:${inputKeys
          .zip(inputs)
          .map { case (k, v) => s"\n$inset$inset- $k: $v" }
          .mkString}
           |$inset- Outputs:${outputs.map { case (k, v) =>
          s"\n$inset$inset- $k: $v"
        }.mkString}""".stripMargin
      }
      s"""DmnEvalRowResult
         |- Status: $status
         |- DMN Table: $decisionId
         |- Test Inputs: ${testInputs.map { case (k, v) =>
        s"\n   - $k: $v"
      }.mkString}${if (matchedRules.isEmpty)
        "\n- There is no Matching Rule for these input(s)"
      else
        matchedRules.map { mr => s"\n${matchRule(mr)}" }.mkString}
         |- Error: ${maybeError.map { e => s"\n   - ${e.msg}" }.getOrElse("-")}
         |""".stripMargin
    }

  private[runner] def testMethod(
      dmnEvalRow: DmnEvalRowResult,
      dmnEvalResult: DmnEvalResult
  ): UIO[String] =
    info(dmnEvalRow, dmnEvalResult)
      .map(info =>
        testMethod(
          methodName(dmnEvalRow.testInputs),
          dmnEvalRow.status match {
            case EvalStatus.INFO =>
              dmnEvalResult.dmn.dmnConfig
                .findTestCase(dmnEvalRow.testInputs)
                .map { case TestCase(_, results) =>
                  (for {
                    MatchedRule(_, _, outputs) <- dmnEvalRow.matchedRules
                    TestResult(_, resultOutputs) <- results
                    test <- outputs.map { case (key, v) =>
                      val value = resultOutputs
                        .get(key)
                        .map(_.valueStr)
                        .getOrElse(s"There is no Output Key '$key''")
                      s"""assertEquals(\"\"\"$info\"\"\", "$value", "$v")"""
                    }

                  } yield test).mkString("\n")
                }
                .getOrElse(
                  s"""assertTrue(\"\"\"$info\"\"\", true)"""
                )
            case status =>
              s"""fail(\"\"\"Dmn Table '${dmnEvalRow.decisionId}' failed with Status $status:
               |$info\"\"\")""".stripMargin
          }
        )
      )

  private[runner] def methodName(testInputs: Map[String, String]) = {
    "test__" + testInputs
      .map { case (k, v) =>
        s"${k}__${v.replaceAll("""[\W]""", "_").take(10)}"
      }
      .mkString("__")
  }

  private[runner] def testMethod(evalException: EvalException): UIO[String] =
    UIO(
      testMethod(
        "evalException",
        s"""fail(\"\"\"Dmn Table '${evalException.decisionId}' failed with:\\n - ${evalException.msg}\"\"\")"""
      )
    )

  private[runner] def testMethod(name: String, content: String) =
    s"""  @Test
         |  def $name(): Unit = {
         |    $content
         |  }
         |""".stripMargin

  private[runner] def testFile(
      className: String,
      testMethods: String*
  ) = {
    for {
      genClass <- UIO(s"""package ${config.packageName}
                         |
                         |import org.junit.Assert._
                         |import org.junit.Test
                         |
                         |class $className {
                         |
                         |${testMethods.mkString("\n")}
                         |}""".stripMargin)
      filePath <- UIO(
        osPath(config.outputPath) / config.packageName.split(
          """\."""
        ) / s"$className.scala"
      )
      _ <- ZIO(rm ! filePath)
      _ <- ZIO(write(filePath, genClass, createFolders = true))
    } yield genClass
  }

}
