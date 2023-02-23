package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.{EvalContext, Failure}
import org.camunda.dmn.parser._
import org.camunda.feel
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
       |  case class In(${createInputs(inputs.toSeq, rules.toSeq)})
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

  private def createInputs(inputs: Seq[ParsedInput], parsedRules: Seq[ParsedRule]) = {
    inputs.map{
      case ParsedInput(id, name, expr) =>
        s"${name}: ${evalExpression(expr)}"

    }.mkString(", ")
/*
    parsedRules.headOption.map{ pr=>
      pr.inputEntries.zip(inputs)
    }
    inputs.map(in => s"${in.expression}: Any")
    .mkString(", ")
 */
  }

  private def evalExpression(expr: ParsedExpression): String =
    expr match {
      case FeelExpression(exp) => evalFeel(exp.expression)
      case EmptyExpression => ""
      case ExpressionFailure(failure) => failure
      case other =>
        s"Failed to evaluate expression '$other'"
    }

  def evalFeel(expression: feel.syntaxtree.Exp): String = {
    import feel.syntaxtree._
    expression match {

      // literals
      case ConstNull => "null = ???"
      case ConstInputValue => "Any = ???"
      case ConstNumber(x) => s"BigDecimal = $x"
      case ConstBool(b) => s"Boolean = $b"
      case ConstString(s) => s"String = $s"
      case ConstDate(d) => s"LocalDate = new LocalDate(\"$d\")"
      /*
      case ConstLocalTime(t) => ValLocalTime(t)
      case ConstTime(t) => ValTime(t)
      case ConstLocalDateTime(dt) => ValLocalDateTime(dt)
      case ConstDateTime(dt) => ValDateTime(dt)
      case ConstYearMonthDuration(d) => ValYearMonthDuration(d.normalized)
      case ConstDayTimeDuration(d) => ValDayTimeDuration(d)

      case ConstList(items) =>
        mapEither[Exp, Val](items, item => eval(item).toEither, ValList)

      case ConstContext(entries) =>
        foldEither[(String, Exp), EvalContext](
          EvalContext.wrap(Context.EmptyContext)(context.valueMapper),
          entries, {
            case (ctx, (key, value)) =>
              eval(value)(context + ctx).toEither.map(v => ctx + (key -> v))
          },
          ValContext
        )

      case range: ConstRange => toRange(range)

      // simple unary tests
      case InputEqualTo(x) =>
        withVal(input, i => checkEquality(i, eval(x), _ == _, ValBoolean))
      case InputLessThan(x) =>
        withVal(input, i => dualOp(i, eval(x), _ < _, ValBoolean))
      case InputLessOrEqual(x) =>
        withVal(input, i => dualOp(i, eval(x), _ <= _, ValBoolean))
      case InputGreaterThan(x) =>
        withVal(input, i => dualOp(i, eval(x), _ > _, ValBoolean))
      case InputGreaterOrEqual(x) =>
        withVal(input, i => dualOp(i, eval(x), _ >= _, ValBoolean))
      case InputInRange(range@ConstRange(start, end)) =>
        unaryOpDual(eval(start.value),
          eval(end.value),
          isInRange(range),
          ValBoolean)

      case UnaryTestExpression(x) => withVal(eval(x), unaryTestExpression)

      // arithmetic operations
      case Addition(x, y) => withValOrNull(addOp(eval(x), eval(y)))
      case Subtraction(x, y) => withValOrNull(subOp(eval(x), eval(y)))
      case Multiplication(x, y) => withValOrNull(mulOp(eval(x), eval(y)))
      case Division(x, y) => withValOrNull(divOp(eval(x), eval(y)))
      case Exponentiation(x, y) =>
        withValOrNull(
          dualNumericOp(eval(x),
            eval(y),
            (x, y) =>
              if (y.isWhole) {
                x.pow(y.toInt)
              } else {
                math.pow(x.toDouble, y.toDouble)
              },
            ValNumber))
      case ArithmeticNegation(x) =>
        withValOrNull(withNumber(eval(x), x => ValNumber(-x)))

      // dual comparators
      case Equal(x, y) => checkEquality(eval(x), eval(y), _ == _, ValBoolean)
      case LessThan(x, y) => dualOp(eval(x), eval(y), _ < _, ValBoolean)
      case LessOrEqual(x, y) => dualOp(eval(x), eval(y), _ <= _, ValBoolean)
      case GreaterThan(x, y) => dualOp(eval(x), eval(y), _ > _, ValBoolean)
      case GreaterOrEqual(x, y) => dualOp(eval(x), eval(y), _ >= _, ValBoolean)

      // combinators
      case AtLeastOne(xs) => atLeastOne(xs, ValBoolean)
      case Not(x) => withBooleanOrNull(eval(x), x => ValBoolean(!x))
      case Disjunction(x, y) => atLeastOne(x :: y :: Nil, ValBoolean)
      case Conjunction(x, y) => all(x :: y :: Nil, ValBoolean)

      // control structures
      case If(condition, statement, elseStatement) =>
        withBooleanOrFalse(eval(condition),
          isMet =>
            if (isMet) {
              eval(statement)
            } else {
              eval(elseStatement)
            })
      case In(x, test) =>
        withVal(eval(x), x => eval(test)(context + (inputKey -> x)))
      case InstanceOf(x, typeName) =>
        withVal(eval(x), x => {
          typeName match {
            case "Any" if x != ValNull => ValBoolean(true)
            case _ => withType(x, t => ValBoolean(t == typeName))
          }
        })
*/
      // context
      case Ref(names) => names.head
 /*     case PathExpression(exp, key) => withVal(eval(exp), v => path(v, key))

      // list
      case SomeItem(iterators, condition) =>
        withCartesianProduct(
          iterators,
          p =>
            atLeastOne(p.map(vars => () => eval(condition)(context ++ vars)),
              ValBoolean))
      case EveryItem(iterators, condition) =>
        withCartesianProduct(
          iterators,
          p =>
            all(p.map(vars => () => eval(condition)(context ++ vars)),
              ValBoolean))
      case For(iterators, exp) =>
        withCartesianProduct(
          iterators,
          p =>
            ValList((List[Val]() /: p) {
              case (partial, vars) => {
                val iterationContext = context ++ vars + ("partial" -> partial)
                val value = eval(exp)(iterationContext)
                partial ++ (value :: Nil)
              }
            })
        )
      case Filter(list, filter) =>
        withList(
          eval(list),
          l => {
            val evalFilterWithItem =
              (item: Val) => eval(filter)(filterContext(item))

            filter match {
              case ConstNumber(index) => filterList(l.items, index)
              case ArithmeticNegation(ConstNumber(index)) =>
                filterList(l.items, -index)
              case _: Comparison | _: FunctionInvocation |
                   _: QualifiedFunctionInvocation =>
                filterList(l.items, evalFilterWithItem)
              case _ =>
                eval(filter) match {
                  case ValNumber(index) => filterList(l.items, index)
                  case _ => filterList(l.items, evalFilterWithItem)
                }
            }
          }
        )
      case IterationContext(start, end) =>
        withNumbers(eval(start), eval(end), (x, y) => {
          val range = if (x < y) {
            (x to y).by(1)
          } else {
            (x to y).by(-1)
          }
          ValList(range.map(ValNumber).toList)
        })

      // functions
      case FunctionInvocation(name, params) =>
        withFunction(findFunction(context, name, params),
          f => invokeFunction(f, params))
      case QualifiedFunctionInvocation(path, name, params) =>
        withContext(
          eval(path),
          c =>
            withFunction(
              findFunction(EvalContext.wrap(c.context)(context.valueMapper),
                name,
                params),
              f => invokeFunction(f, params)))
      case FunctionDefinition(params, body) =>
        ValFunction(
          params,
          paramValues =>
            body match {
              case JavaFunctionInvocation(className, methodName, arguments) =>
                invokeJavaFunction(className,
                  methodName,
                  arguments,
                  paramValues,
                  context.valueMapper)
              case _ => eval(body)(context ++ (params zip paramValues).toMap)
            }
        )

      // unsupported expression
      case exp => ValError(s"unsupported expression '$exp'")
*/
      case other => s"$other - ${other.getClass}"

    }
  }

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
