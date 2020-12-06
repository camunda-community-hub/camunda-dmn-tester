package pme123.camunda.dmn.tester.shared

trait DmnApi {
  // get DmnConfigs items
  def getConfigs(path: Seq[String]): Seq[DmnConfig]
}
