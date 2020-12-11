package pme123.camunda.dmn.tester.client.config

import autowire.{clientCallable, _}
import boopickle.Default._
import pme123.camunda.dmn.tester.client.config.ConfigItem.activeCheck
import pme123.camunda.dmn.tester.client.services.AjaxClient
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig}
import slinky.core.FunctionalComponent
import slinky.core.facade.Fragment
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.web.html.{div, style}
import typings.antd.antdStrings
import typings.antd.antdStrings.{center, middle}
import typings.antd.components._

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
      val (error, setError) = useState[Option[String]](None)
      val (isLoaded, setIsLoaded) = useState(false)
      val (isActive, setIsActive) = useState(false)
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
                setIsLoaded(true)
                setBasePath(path)
              case Failure(ex) =>
                setIsLoaded(true)
                setError(Some(ex.toString))
            }
        },
        Seq.empty
      )

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

      lazy val handleConfigToggle = { (config: DmnConfig) =>
        val newCF = config.copy(isActive = !config.isActive)
        setConfigs(configs.map {
          case c if c.decisionId == config.decisionId =>
            newCF
          case c => c
        })
      /* AjaxClient[DmnApi]
            .updateConfig(config.copy(isActive = !config.isActive))
            .call()
            .onComplete {
              case Success(configs) =>
                setConfigs(configs)
              case Failure(_) =>
                message.error(s"Problem update $config", 10)
            }*/
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
            Card
              .title(
                Fragment(
                  "2. Select the DMN Configurations you want to test.",
                  div(style := literal(textAlign = "right", marginRight = 10))(
                    activeCheck(isActive = isActive, active => {
                      setIsActive(active)
                      setConfigs(configs.map(_.copy(isActive = active)))
                    })
                  )
                )
              )(
                (error, isLoaded) match {
                  case (Some(msg), _) =>
                    Alert
                      .message(
                        s"Error: The DMN Configurations could not be loaded. (is the path ok?)"
                      )
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
                    ConfigList(configs, handleConfigToggle)
                }
              )
          )
        )
    }

  private def col(card: Card.Builder) =
    Col
      .xs(23)
      .sm(23)
      .md(21)
      .lg(20)
      .xl(18)(
        card
      )
}
