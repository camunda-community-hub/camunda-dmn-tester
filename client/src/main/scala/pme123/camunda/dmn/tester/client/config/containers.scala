package pme123.camunda.dmn.tester.client.config

import autowire.{clientCallable, _}
import boopickle.Default._
import pme123.camunda.dmn.tester.client.services.AjaxClient
import pme123.camunda.dmn.tester.client.config.components.TList
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig}
import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks.{useEffect, useState}
import typings.antd.antdStrings
import typings.antd.antdStrings.{center, middle}
import typings.antd.components._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

@JSImport("resources/Config.css", JSImport.Default)
@js.native
object ConfigCSS extends js.Object

object containers {
  private val css = ConfigCSS

  val DmnConfigContainer: FunctionalComponent[Unit] =
    FunctionalComponent[Unit] { _ =>
      val (error, setError) = useState[Option[String]](None)
      val (isLoaded, setIsLoaded) = useState(false)
      val (configs, setItems) = useState(Seq.empty[DmnConfig])

      // Note: the empty deps array [] means
      // this useEffect will run once
      useEffect(() => handleFormSubmit(configPaths.head), Seq.empty)

      lazy val handleFormSubmit =
        (path: String) => {
          val pathSeq = path.split("/").filter(_.trim.nonEmpty)
          AjaxClient[DmnApi]
            .getConfigs(pathSeq)
            .call()
            .onComplete {
              case Success(configs) =>
                setIsLoaded(true)
                setItems(configs)
              case Failure(ex) =>
                setIsLoaded(true)
                setError(Some(ex.toString))
            }
        }

      lazy val handleConfigToggle =
        (config: DmnConfig) =>
          /*  AjaxClient[DmnApi].updateConfig(config.copy(completed = !config.completed)).call().foreach { configs =>
            setItems(configs)
          }*/ ()
      lazy val handleRemoveConfig =
        (config: DmnConfig) =>
          /*   AjaxClient[DmnApi].deleteConfig(config.id.get).call().foreach { configs =>
            setItems(configs)
          }*/ ()
      Row
        // .gutter(20) //[0, 20] ?
        .justify(center)
        .className("configs-container")
        .align(middle)(
          Col
            .xs(23)
            .sm(23)
            .md(21)
            .lg(20)
            .xl(18)(
              Card(
                ChangeConfigsForm(configPaths.head, handleFormSubmit)
              )
            ),
          Col
            .xs(23)
            .sm(23)
            .md(21)
            .lg(20)
            .xl(18)(
              Card
                .title("DMN Configuration")(
                  (error, isLoaded) match {
                    case (Some(msg), _) =>
                      Alert
                        .message(s"Error: The DMN Configurations could not be loaded. (is the path ok?)")
                        .`type`(antdStrings.error)
                        .showIcon(true)
                    case (_, false) =>
                      Spin
                        .size(antdStrings.default)
                        .spinning(true)(
                          Alert
                            .message("Loading Configs")
                            .`type`(antdStrings.info)
                            .showIcon(true)
                        )
                    case _ =>
                      TList(configs, handleConfigToggle)
                  }
                )
            )
        )
    }
}
