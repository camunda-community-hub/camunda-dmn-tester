package pme123.camunda.dmn.tester.server.runner

import pme123.camunda.dmn.tester.shared.{DmnConfig, TesterData, TesterInput}
import zio.ZIO
import zio.config.typesafe.TypesafeConfigSource
import zio.test.Assertion.{containsString, equalTo}
import zio.test.{DefaultRunnableSpec, assert, suite, testM}
import pme123.camunda.dmn.tester.shared.conversions._

//noinspection TypeAnnotation
object DmnConfigHandlerSpec extends DefaultRunnableSpec {
  val config =
    """
      |decisionId: country-risk,
      |dmnPath: [core, src, test, resources, country-risk.dmn],
      |data: {
      |  inputs: [{
      |    key: currentCountry,
      |    values: [CH, ch, DE, OTHER, an awful long Input that should be cutted]
      |  }, {
      |    key: someNumber,
      |    values: [12, 12.5, 13]
      |  }, {
      |    key: someBoolean,
      |    values: [true, false]
      |  }]
      |}
      |""".stripMargin

  val config1 = DmnConfig("country-risk",
    TesterData(List(TesterInput("currentCountry", List("CH", "ch", "DE", "OTHER", "an awful long Input that should be cutted")))),
    List("core", "src", "test", "resources", "country-risk.dmn"))

  def spec =
    suite("DmnConfigHandlerSpec")(
      testM("read and write DmnConfig") {
        for {
          dmnSource <- ZIO.fromEither(TypesafeConfigSource.fromHoconString(config))
          dmnConfig <- hocon.readConfig(dmnSource)
          _ <- zio.console.putStrLn(s"DmnConfig: $dmnConfig")
          configStr <- hocon.writeConfig(dmnConfig)
        } yield {
          assert(dmnConfig)(
            equalTo(config1)
          ) &&
          assert(configStr)(
          containsString("12,") &&
            containsString("true,") &&
            containsString("\"an awful")
        )
        }
      }
    )

}
