package pme123.camunda.dmn.tester.shared

trait DmnApi {
  // get DmnConfigs items
  def getConfigs(path: Seq[String]): Seq[DmnConfig]

  // gets the absolute path of the server
  def getBasePath(): String
  // update a Config
 // def updateConfig(item: DmnConfig): Seq[DmnConfig]

  // delete a Config
 // def deleteConfig(itemId: String): Seq[DmnConfig]
}
