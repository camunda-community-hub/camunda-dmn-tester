package pme123.camunda.dmn.tester.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AllDmnTables(
    dmnConfig: DmnConfig,
    tables: Seq[DmnTable]
) {
  lazy val mainTable: DmnTable = tables.head
  lazy val requiredTables: Seq[DmnTable] = tables.tail

  lazy val hasRequiredTables: Boolean = requiredTables.nonEmpty
  def isMainTable(decisionId: String): Boolean =
    mainTable.decisionId == decisionId
}

object AllDmnTables {
  implicit val decoder: Decoder[AllDmnTables] = deriveDecoder
  implicit val encoder: Encoder[AllDmnTables] = deriveEncoder
}

case class DmnTable(
    decisionId: String,
    name: String,
    hitPolicy: HitPolicy,
    aggregation: Option[Aggregator],
    inputCols: Seq[InputColumn],
    outputCols: Seq[OutputColumn],
    ruleRows: Seq[DmnRule]
)
object DmnTable {
  implicit val decoder: Decoder[DmnTable] = deriveDecoder
  implicit val encoder: Encoder[DmnTable] = deriveEncoder
}

case class InputColumn(
    name: String,
    feelExprText: String
)
object InputColumn {
  implicit val decoder: Decoder[InputColumn] = deriveDecoder
  implicit val encoder: Encoder[InputColumn] = deriveEncoder
}

case class OutputColumn(
    name: String,
    value: Option[String]
)
object OutputColumn {
  implicit val decoder: Decoder[OutputColumn] = deriveDecoder
  implicit val encoder: Encoder[OutputColumn] = deriveEncoder
}

case class DmnRule(
    index: Int,
    ruleId: String,
    inputs: Seq[(String, String)],
    outputs: Seq[(String, String)]
)
object DmnRule {
  implicit val decoder: Decoder[DmnRule] = deriveDecoder
  implicit val encoder: Encoder[DmnRule] = deriveEncoder
}

sealed trait HitPolicy {
  def isSingle: Boolean
}

object HitPolicy {

  case object UNIQUE extends HitPolicy {
    val isSingle = true
  }

  case object FIRST extends HitPolicy {
    val isSingle = true
  }

  case object ANY extends HitPolicy {
    val isSingle = true
  }

  case object COLLECT extends HitPolicy {
    val isSingle = false
  }

  def apply(value: String): HitPolicy =
    value.toUpperCase match {
      case "UNIQUE"  => UNIQUE
      case "FIRST"   => FIRST
      case "ANY"     => ANY
      case "COLLECT" => COLLECT
    }

  implicit val decoder: Decoder[HitPolicy] = deriveDecoder
  implicit val encoder: Encoder[HitPolicy] = deriveEncoder

}

sealed trait Aggregator

object Aggregator {

  case object SUM extends Aggregator
  case object COUNT extends Aggregator
  case object MIN extends Aggregator
  case object MAX extends Aggregator

  def apply(value: String): Aggregator =
    value.toUpperCase match {
      case "SUM"   => SUM
      case "COUNT" => COUNT
      case "MIN"   => MIN
      case "MAX"   => MAX
    }

  implicit val decoder: Decoder[Aggregator] = deriveDecoder
  implicit val encoder: Encoder[Aggregator] = deriveEncoder

}
