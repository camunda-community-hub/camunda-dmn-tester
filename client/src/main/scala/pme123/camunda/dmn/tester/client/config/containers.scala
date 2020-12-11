package pme123.camunda.dmn.tester.client.config

import autowire.{clientCallable, _}
import boopickle.Default._
import pme123.camunda.dmn.tester.client.services.AjaxClient
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig, EvalResult}
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.core.{FunctionalComponent, SyntheticEvent, TagMod}
import typings.antd.antdStrings.{center, middle, primary}
import typings.antd.components._
import typings.antd.mod.message

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
      val (maybeConfigsError, setConfigsError) = useState[Option[String]](None)
      val (isConfigsLoaded, setIsConfigsLoaded) = useState(false)
      val (configs, setConfigs) = useState(Seq.empty[DmnConfig])
      val (maybeEvalResultsError, setEvalResultsError) =
        useState[Option[String]](None)
      val (isEvalResultsLoaded, setIsEvalResultsLoaded) = useState(true)
      val (evalResults, setEvalResults) = useState(Seq.empty[EvalResult])
      val (basePath, setBasePath) = useState("")

      // Note: the empty deps array [] means
      // this useEffect will run once
      useEffect(
        () => {
          loadingConfigs(configPaths.head)
          AjaxClient[DmnApi]
            .getBasePath()
            .call()
            .onComplete {
              case Success(path) =>
                setBasePath(path)
              case Failure(ex) =>
                message.error(s"Problem loading base path: ${ex.toString}")
            }
        },
        Seq.empty
      )

      lazy val runTests = (_: SyntheticEvent[_, _]) => {
        setIsEvalResultsLoaded(false)
        AjaxClient[DmnApi]
          .runTests(configs.filter(_.isActive))
          .call()
          .onComplete {
            case Success(testResults) =>
              setIsEvalResultsLoaded(true)
              setEvalResults(testResults)
            case Failure(ex) =>
              setIsEvalResultsLoaded(true)
              setEvalResultsError(Some(s"Problem running the Tests: ${ex.toString}"))
          }
      }

      lazy val loadingConfigs =
        (path: String) => {
          setIsConfigsLoaded(false)
          val pathSeq = path.split("/").filter(_.trim.nonEmpty)
          AjaxClient[DmnApi]
            .getConfigs(pathSeq)
            .call()
            .onComplete {
              case Success(configs) =>
                setIsConfigsLoaded(true)
                setConfigs(configs)
              case Failure(ex) =>
                setIsConfigsLoaded(true)
                setConfigsError(Some(s"Problem loading DMN Configs: ${ex.toString}"))
            }
        }

      Row
        // .gutter(20) //[0, 20] ?
        .justify(center)
        .className("configs-container")
        .align(middle)(
          col(
            Card
              .title("1. Select Path where your DMN Configurations are.")(
                ChangeConfigsForm(basePath, loadingConfigs)
              )
          ),
          col(
            ConfigCard(configs, isConfigsLoaded, maybeConfigsError, setConfigs)
          ),
          col(
            Card
              .title("3. Run the Tests.")(
                Button
                  .`type`(primary)
                  .block(true)
                  .onClick(runTests)("Run it")
              )
          ),
          col(
            EvalResultsCard(evalResults, isEvalResultsLoaded, maybeEvalResultsError)
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
