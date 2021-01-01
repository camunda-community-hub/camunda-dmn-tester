package pme123.camunda.dmn.tester.client.runner

import autowire._
import boopickle.Default._
import pme123.camunda.dmn.tester.client.services.AjaxClient
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.{DmnApi, DmnConfig, DmnEvalResult}
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.core.{FunctionalComponent, SyntheticEvent, TagMod}
import typings.antd.antdStrings.{center, middle, primary}
import typings.antd.components._
import typings.antd.mod.message
import pme123.camunda.dmn.tester.client._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
      val (evalResults, setEvalResults) =
        useState(Seq.empty[Either[EvalException, DmnEvalResult]])
      val (basePath, setBasePath) = useState("")
      val (activePath, setActivePath) = useState(Seq.empty[String])

      // Note: the empty deps array [] means
      // this useEffect will run once
      useEffect(
        () => {
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
              setEvalResultsError(None)
            case Failure(ex) =>
              setIsEvalResultsLoaded(true)
              setEvalResultsError(
                Some(s"Problem running the Tests: ${ex.toString}")
              )
          }
      }

      lazy val loadingConfigs =
        (path: String) => {
          val pathSeq = path.split("/").filter(_.trim.nonEmpty)
          setActivePath(pathSeq)
          evaluateConfigs(
            AjaxClient[DmnApi]
              .getConfigs(pathSeq)
              .call()
          )
        }

      lazy val addDmnConfig = (dmnConfig: DmnConfig) => {
        evaluateConfigs(
          AjaxClient[DmnApi]
            .addConfig(dmnConfig, activePath)
            .call()
        )
      }

      lazy val editDmnConfig = (dmnConfig: DmnConfig) => {
        evaluateConfigs(
          AjaxClient[DmnApi]
            .updateConfig(dmnConfig, activePath)
            .call()
        )
      }

      lazy val deleteDmnConfig = (dmnConfig: DmnConfig) => {
        evaluateConfigs(
          AjaxClient[DmnApi]
            .deleteConfig(dmnConfig, activePath)
            .call()
        )
      }

      lazy val evaluateConfigs = (configs: Future[Seq[DmnConfig]]) => {
        setIsConfigsLoaded(false)
        configs
          .onComplete {
            case Success(configs) =>
              setIsConfigsLoaded(true)
              setConfigs(configs)
              setConfigsError(None)
            case Failure(ex) =>
              setIsConfigsLoaded(true)
              setConfigsError(
                Some(s"Problem loading DMN Configs: ${ex.toString}")
              )
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
            ConfigCard(
              basePath,
              configs,
              isConfigsLoaded,
              maybeConfigsError,
              setConfigs,
              addDmnConfig,
              editDmnConfig,
              deleteDmnConfig
            )
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
            EvalResultsCard(
              evalResults,
              isEvalResultsLoaded,
              maybeEvalResultsError,
              editDmnConfig
            )
          )
        )
    }
}
