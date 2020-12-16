package pme123.camunda.dmn.tester.shared

import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException

trait DmnApi {
  // get DmnConfigs items
  def getConfigs(path: Seq[String]): Seq[DmnConfig]

  // gets the absolute path of the server
  def getBasePath(): String

  def runTests(configs: Seq[DmnConfig]): Seq[Either[EvalException, DmnEvalResult]]
  // update a Config
 // def updateConfig(item: DmnConfig): Seq[DmnConfig]

  // delete a Config
 // def deleteConfig(itemId: String): Seq[DmnConfig]
}
