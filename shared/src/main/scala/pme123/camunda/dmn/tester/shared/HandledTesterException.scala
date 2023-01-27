package pme123.camunda.dmn.tester.shared

sealed trait HandledTesterException {
  def msg: String
}
object HandledTesterException {
  case class ConfigException(msg: String) extends HandledTesterException
  case class EvalException(decisionId: String, msg: String) extends HandledTesterException
  case class ConsoleException(msg: String) extends HandledTesterException
}

