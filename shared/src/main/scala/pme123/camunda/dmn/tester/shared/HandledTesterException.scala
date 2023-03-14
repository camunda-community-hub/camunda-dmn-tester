package pme123.camunda.dmn.tester.shared

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

sealed trait HandledTesterException {
  def msg: String
}
object HandledTesterException {
  case class ConfigException(msg: String) extends HandledTesterException
  case class DmnException(msg: String) extends HandledTesterException
  case class EvalException(dmnConfig: DmnConfig, msg: String) extends HandledTesterException
  case class ConsoleException(msg: String) extends HandledTesterException
  case class DecisionDmnCreatorException(msg: String) extends HandledTesterException

  object EvalException {
    implicit val decoder: Decoder[EvalException] = deriveDecoder
    implicit val encoder: Encoder[EvalException] = deriveEncoder
  }
  implicit val decoder: Decoder[HandledTesterException] = deriveDecoder
  implicit val encoder: Encoder[HandledTesterException] = deriveEncoder

}

