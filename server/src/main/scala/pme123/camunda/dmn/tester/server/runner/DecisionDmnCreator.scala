package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.Failure
import org.camunda.dmn.parser._
import pme123.camunda.dmn.tester.server.DmnService
import pme123.camunda.dmn.tester.shared.HandledTesterException.{ConfigException, DecisionDmnCreatorException, EvalException}
import zio.{IO, ZIO}

import java.io.InputStream
import scala.util.Try

case class DecisionDmnCreator(dmnPath: os.Path) {

  def run(): IO[DecisionDmnCreatorException, String] = {
    val caseClasses = for {
      parsedDmn <- parseDmn()
    } yield parsedDmn.decisions
      .collect {
        case ParsedDecision(decisionId, _, parsedDecisionTable: ParsedDecisionTable, _, _, _, _) =>
          createCaseClass(decisionId, parsedDecisionTable)
      }.mkString("\n")
    caseClasses.map(imports + _)
  }

  private def createCaseClass(decisionId: String, parsedDecisionTable: ParsedDecisionTable) = {
    val       ParsedDecisionTable(
        inputs,
        outputs,
        rules,
        hitPolicy,
        aggregator
      ) = parsedDecisionTable
    val className = decisionId.head.toUpper + decisionId.tail

    s"""
       |/*
       |* Hit Policy: $hitPolicy
       |* Aggregator: ${Option(aggregator).getOrElse("-")}
       |*/
       |object $className:
       |  case class In(${createInputs(inputs.toSeq)})
       |  object In:
       |    given Schema[In] = Schema.derived
       |    given Encoder[In] = deriveEncoder
       |    given Decoder[In] = deriveDecoder
       |  end In
       |  case class Out()
       |  object Out:
       |    given Schema[Out] = Schema.derived
       |    given Encoder[Out] = deriveEncoder
       |    given Decoder[Out] = deriveDecoder
       |  end Out
       |end ${className}
       |""".stripMargin
  }

  private def parseDmn(): IO[DecisionDmnCreatorException, ParsedDmn] =
    for {
      is <- ZIO
        .fromTry(Try(os.read.inputStream(dmnPath)))
        .orElseFail(
          DecisionDmnCreatorException(
            s"There was no DMN in ${dmnPath.toIO.getAbsolutePath}."
          )
        )
      dmn <- parsedDmn(is)
    } yield dmn

  private lazy val engine: DmnEngine = new DmnEngine()

  private def parsedDmn(
      streamToTest: InputStream
  ): IO[DecisionDmnCreatorException, ParsedDmn] = {
    ZIO
      .fromEither(engine.parse(streamToTest))
      .mapError {
        case Failure(message) if message.contains(feelParseErrMsg) =>
          DecisionDmnCreatorException(feelParseErrHelp(message))
        case Failure(msg) => DecisionDmnCreatorException(msg)
      }
  }

  private def createInputs(inputs: Seq[ParsedInput]) =
    inputs.map(in => s"${in.expression}: Any").mkString(", ")
  private lazy val imports =
    """
      |import camundala.domain.*
      |""".stripMargin
}

object DecisionDmnCreator extends App {
  val dmnPath = os.pwd / "server" / "src" / "test" / "resources" / "dinnerDecisions.dmn"

  println(
    DmnService.createCaseClasses(dmnPath)
  )
}
