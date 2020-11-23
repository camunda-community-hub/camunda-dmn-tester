package pme123.camunda.dmn.tester

import pme123.camunda.dmn.HandledTesterException
import pme123.camunda.dmn.tester.EvalStatus._
import zio.test.Assertion.{
  endsWithString,
  equalTo,
  hasField,
  isSubtype,
  startsWithString
}
import zio.test.{DefaultRunnableSpec, assert, suite, testM}

object TestRunnerSpec extends DefaultRunnableSpec {

  def spec =
    suite("TestRunnerSpec")(
      testM("runner works without failures") {
        for {
          result <- TestRunner.runApp(RunnerConfig.defaultConfig)
        } yield (assert(result.size)(equalTo(29)) &&
          (assert(result.count(_.status == WARN))(equalTo(1))) &&
          (assert(result.count(_.status == ERROR))(equalTo(1))))
      },
      testM("runner works with non existing Config path") {
        for {
          result <- TestRunner
            .runApp(RunnerConfig(RunnerConfig.defaultBasePath :+ "bad"))
            .flip
        } yield assert(result)(
          isSubtype[HandledTesterException](
            hasField(
              "msg",
              _.msg,
              startsWithString(
                "Your provided Config Path does not exist ("
              ) && endsWithString(
                "camunda-dmn-tester/core/src/test/resources/dmn-configs/bad)."
              )
            )
          )
        )
      },
      testM("runner works with file instead of directory") {
        for {
          result <- TestRunner
            .runApp(
              RunnerConfig(RunnerConfig.defaultBasePath :+ "numbers.conf")
            )
            .flip
        } yield assert(result)(
          isSubtype[HandledTesterException](
            hasField(
              "msg",
              _.msg,
              startsWithString(
                "Your provided Config Path is not a directory (/"
              ) && endsWithString(
                "camunda-dmn-tester/core/src/test/resources/dmn-configs/numbers.conf)."
              )
            )
          )
        )
      }
    )

}
