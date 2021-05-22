package pme123.camunda.dmn.tester.server.runner

import zio.ZIO
import zio.config.typesafe.TypesafeConfigSource
import zio.test.Assertion.containsString
import zio.test.{DefaultRunnableSpec, assert, suite, testM}

//noinspection TypeAnnotation
object DmnConfigHandlerSpec extends DefaultRunnableSpec {
  val config =
    """
      |decisionId: country-risk,
      |dmnPath: [server, src, test, resources, country-risk.dmn],
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
      |  variables: []
      |}
      |""".stripMargin

  def spec =
    suite("DmnConfigHandlerSpec")(
      testM("read and write DmnConfig") {
        for {
          dmnSource <- ZIO.fromEither(TypesafeConfigSource.fromHoconString(config))
          dmnConfig <- hocon.readConfig(dmnSource)
          _ <- zio.console.putStrLn(s"DmnConfig: $dmnConfig")
          configStr <- hocon.writeConfig(dmnConfig)
        } yield {
          assert(configStr)(
          containsString("\"12\",") && // string because zio-config makes strings if writing hocon
            containsString("\"true\",") &&
            containsString("\"an awful")
        )
        }
      }
    )

}
