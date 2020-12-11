package pme123.camunda.dmn.tester.client.config

import autowire.{clientCallable, _}
import boopickle.Default._
import pme123.camunda.dmn.tester.client.services.AjaxClient
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig}
import slinky.core.{FunctionalComponent, SyntheticEvent, TagMod}
import slinky.core.facade.Fragment
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.web.html.{div, style}
import typings.antd.antdStrings
import typings.antd.antdStrings.{center, middle, primary}
import typings.antd.components._
import typings.antd.mod.message

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

@JSImport("resources/Config.css", JSImport.Default)
@js.native
object ConfigCSS extends js.Object

object containers {
  private val css = ConfigCSS

  val DmnConfigContainer: FunctionalComponent[Unit] =
    FunctionalComponent[Unit] { _ =>
      val (maybeError, setError) = useState[Option[String]](None)
      val (isLoaded, setIsLoaded) = useState(false)
      val (configs, setConfigs) = useState(Seq.empty[DmnConfig])
      val (basePath, setBasePath) = useState("")

      // Note: the empty deps array [] means
      // this useEffect will run once
      useEffect(
        () => {
          handleFormSubmit(configPaths.head)
          AjaxClient[DmnApi]
            .getBasePath()
            .call()
            .onComplete {
              case Success(path) =>
                setBasePath(path)
              case Failure(ex) =>
                message.error(ex.toString)
            }
        },
        Seq.empty
      )

      lazy val runTests = (_: SyntheticEvent[_, _]) => {
        println("RUN TESTS")
        AjaxClient[DmnApi]
          .runTests(configs.filter(_.isActive))
          .call()
          .onComplete {
            case Success(testResults) =>
              println(s"testResults: $testResults")
            case Failure(ex) =>
              message.error(ex.toString)
          }
      }

      lazy val handleFormSubmit =
        (path: String) => {
          val pathSeq = path.split("/").filter(_.trim.nonEmpty)
          AjaxClient[DmnApi]
            .getConfigs(pathSeq)
            .call()
            .onComplete {
              case Success(configs) =>
                setIsLoaded(true)
                setConfigs(configs)
              case Failure(ex) =>
                setIsLoaded(true)
                setError(Some(ex.toString))
            }
        }

      lazy val handleRemoveConfig =
        (config: DmnConfig) =>
          /*   AjaxClient[DmnApi].deleteConfig(config.id.get).call().foreach { configs =>
            setConfigs(configs)
          }*/ ()
      Row
        // .gutter(20) //[0, 20] ?
        .justify(center)
        .className("configs-container")
        .align(middle)(
          col(
            Card
              .title("1. Select Path where your DMN Configurations are.")(
                ChangeConfigsForm(basePath, handleFormSubmit)
              )
          ),
          col(
            ConfigCard(configs, isLoaded, maybeError, setConfigs)
          ),
          col(
            Card
              .title("3. Run the Tests.")(
                Button
                  .`type`(primary)
                  .block(true)
                  .onClick(runTests)("Run it")
              )
          )
        )
    }

  private def col(card: TagMod[slinky.web.html.div.tag.type]) =
    Col
      .xs(23)
      .sm(23)
      .md(21)
      .lg(20)
      .xl(18)(
        card
      )
}
