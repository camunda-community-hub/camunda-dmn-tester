package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.server.runner.osPath
import pme123.camunda.dmn.tester.server.{ZDmnService => z}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.{IO, Runtime, UIO, Unsafe, ZIO}

import scala.util.Try

case class UnitTestGeneratorConfig(
    packageName: String = "pme123.camunda.dmn.tester.test",
    outputPath: List[String] = List("server", "target", "generated-tests")
)
case class DmnUnitTestGenerator(
    config: UnitTestGeneratorConfig = UnitTestGeneratorConfig()
) {
  private val runtime = Runtime.default

  private def run[E, A](body: => IO[E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(body).getOrThrowFiberFailure()
    }

  def generate(): IO[Any, Unit] =
    for {
      configPaths <- z.loadConfigPaths()
      configs <- ZIO.foreach(configPaths)(p =>
        z.loadConfigs(p.split("/").filter(_.trim.nonEmpty))
      )
      testResults <- z.runTests(configs.flatten)
      _ <- ZIO.foreach(testResults)(r => generate(r))
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
          testMethods <- testMethods(result)
          missingMethods <- ZIO.foreach(result.missingRules)(r =>
            missingRuleMethod(r, result.inputKeys)
          )
          _ <- testFile(name, testMethods ++ missingMethods: _*)
        } yield ()
    }

  private[server] def className(decisionId: String) =
    ZIO.succeed(s"${decisionId.trim
      .split("""[\W]""")
      .filter(_.trim.nonEmpty)
      .map(n => n.capitalize)
      .mkString}Suite")

  private[server] def testMethods(dmnEvalResult: DmnEvalResult) = {
    ZIO.foreach(dmnEvalResult.evalResults) { dmnEvalRow =>
      testMethod(dmnEvalRow, dmnEvalResult)
    }
  }

  private[server] def missingRuleMethod(
      dmnRule: DmnRule,
      inputKeys: Seq[String]
  ): UIO[String] =
    ZIO.succeed(
      testMethod(
        s"missing_${dmnRule.index}_${dmnRule.inputs.map(i => if(i == null) "null" else i.replaceAll("""[\W]""", "_")).mkString("__")}",
        s"""fail(\"\"\"There is no Rule that matched for these Inputs:
                       |${inputKeys
          .zip(dmnRule.inputs)
          .map { case (k: String, v: String) =>
            s"|- $k: $v"
          }
          .mkString("\n")}\"\"\".stripMargin)""".stripMargin
      )
    )

  private[server] def info(
      dmnEvalRow: DmnEvalRowResult,
      dmnEvalResult: DmnEvalResult
  ): UIO[String] =
    ZIO.succeed {
      val DmnEvalRowResult(
        status,
        decisionId,
        testInputs,
        matchedRules,
        maybeError
      ) = dmnEvalRow
      val inset: String = "   "
      def matchRule(rule: MatchedRule): String = {
        val MatchedRule(ruleId, ruleIndex, inputs, outputs) = rule
        s"""- Matched Rule: $ruleId (${ruleIndex.intValue})
           |$inset- Inputs:${dmnEvalResult.inputKeys
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

  private[server] def testMethod(
      dmnEvalRow: DmnEvalRowResult,
      dmnEvalResult: DmnEvalResult
  ): UIO[String] =
    info(dmnEvalRow, dmnEvalResult)
      .map(info =>
        testMethod(
          methodName(dmnEvalRow.testInputs),
          dmnEvalRow.status match {
            case EvalStatus.INFO =>
                  successfulTestCase(info)
            case status =>
              s"""fail(\"\"\"Dmn Row of'${dmnEvalRow.decisionId}' failed with Status $status:
               |$info\"\"\")""".stripMargin
          }
        )
      )

  private[server] def successfulTestCase(
      info: String
  ) = {
      s"""assert(true)
             |/*
             |$info
             |*/""".stripMargin
  }

  private[server] def methodName(testInputs: Map[String, String]) = {
    "Test Inputs: " + testInputs
      .map {
        case (k, v) if v == null =>
          s"$k -> null"
        case (k, v) =>
        s"$k -> ${v.take(12)}"
      }
      .mkString(" | ")
  }

  private[server] def testMethod(evalException: EvalException): UIO[String] =
    ZIO.succeed(
      testMethod(
        "evalException",
        s"""fail(\"\"\"Dmn Table '${evalException.decisionId}' failed with:\\n - ${evalException.msg}\"\"\")"""
      )
    )

  private[server] def testMethod(name: String, content: String) =
        s"""  test ("$name") {
         |    $content
         |  }
         |""".stripMargin

  private[server] def testFile(
      className: String,
      testMethods: String*
  ) = {
    for {
      genClass <- ZIO.succeed(s"""package ${config.packageName}
                         |
                         |import org.scalatest.funsuite.AnyFunSuite
                         |
                         |class $className extends AnyFunSuite {
                         |
                         |${testMethods.mkString("\n")}
                         |}""".stripMargin)
      filePath <- ZIO.succeed(
        osPath(config.outputPath) / config.packageName.split(
          """\."""
        ) / s"$className.scala"
      )
      _ <- ZIO.fromTry(Try(os.remove(filePath)))
      _ <- ZIO.fromTry(Try(os.write(filePath, genClass, createFolders = true)))
    } yield genClass
  }

}
