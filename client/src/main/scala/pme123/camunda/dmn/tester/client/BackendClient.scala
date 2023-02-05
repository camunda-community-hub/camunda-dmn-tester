package pme123.camunda.dmn.tester.client

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.scalajs.*
import org.scalajs.dom.XMLHttpRequest
import pme123.camunda.dmn.tester.shared.*
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object BackendClient {

  private lazy val url =
    if (dom.window.location.port == "5173") {
      println(
        "ATTENTION: CORS - see https://stackoverflow.com/a/38000615/2750966"
      )
      "http://localhost:8883"
    } else ""

  // update DmnConfig
  def updateConfig(
      dmnConfig: DmnConfig,
      path: String
  ): EventStream[Either[ErrorMessage, Seq[DmnConfig]]] =
    AjaxEventStream
      .put(
        s"$url/api/dmnConfig?path=${URLEncoder.encode(path, StandardCharsets.UTF_8)}",
        data = dmnConfig.asJson.toString
      )
      .map(extractBody[Seq[DmnConfig]])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              "Problem updating Dmn Config",
              s"Message: ${err.getMessage} "
            )
          )
        )
      )

  // delete DmnConfig
  def deleteConfig(
      dmnConfig: DmnConfig,
      path: String
  ): EventStream[Either[ErrorMessage, Seq[DmnConfig]]] =
    AjaxEventStream
      .delete(
        s"$url/api/dmnConfig?path=${URLEncoder.encode(path, StandardCharsets.UTF_8)}",
        data = dmnConfig.asJson.toString
      )
      .map(extractBody[Seq[DmnConfig]])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              "Problem getting Base Paths",
              s"Message: ${err.getMessage} "
            )
          )
        )
      )

  // gets the absolute path of the server / or where you run the testRunner.sc Script
  def getBasePath: EventStream[Either[ErrorMessage, String]] =
    AjaxEventStream
      .get(s"$url/api/basePath")
      .map(extractBody[String])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              "Problem getting Base Paths",
              s"Message: ${err.getMessage}"
            )
          )
        )
      )
  end getBasePath

  // get configured Paths
  def getConfigPaths: EventStream[Either[ErrorMessage, Seq[String]]] =
    AjaxEventStream
      .get(s"$url/api/configPaths")
      .map(extractBody[Seq[String]])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              "Problem getting Config Paths",
              s"Message: ${err.getMessage}"
            )
          )
        )
      )
  end getConfigPaths

  // get DmnConfigs items
  def getConfigs(
      path: String
  ): EventStream[Either[ErrorMessage, Seq[DmnConfig]]] =
    AjaxEventStream
      .get(
        s"$url/api/dmnConfigs?path=${URLEncoder.encode(path, StandardCharsets.UTF_8)}"
      )
      .map(extractBody[Seq[DmnConfig]])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              "Problem getting Dmn Configs",
              s"Message: ${err.getMessage}"
            )
          )
        )
      )

  end getConfigs

  def validateDmnPath(
      path: String
  ): EventStream[Either[ErrorMessage, Boolean]] =
    AjaxEventStream
      .get(
        s"$url/api/validateDmnPath?path=${URLEncoder.encode(path, StandardCharsets.UTF_8)}"
      )
      .map(extractBody[Boolean])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              s"Problem validating Dmn Path $path",
              s"Message: ${err.getMessage}"
            )
          )
        )
      )

  end validateDmnPath

  def runTests(
      configs: Seq[DmnConfig]
  ): EventStream[
    Either[ErrorMessage, Seq[Either[EvalException, DmnEvalResult]]]
  ] =
    AjaxEventStream
      .post(s"$url/api/runDmnTests", data = configs.asJson.toString)
      .map(extractBody[Seq[Either[EvalException, DmnEvalResult]]])
      .recover(err =>
        Some(
          Left(
            ErrorMessage(
              s"Problem running Dmn Tests",
              s"Message: ${err.getMessage}"
            )
          )
        )
      )

  end runTests

  private def extractBody[T: Decoder](req: XMLHttpRequest) =
    parser
      .parse(req.responseText)
      .flatMap(_.as[T])
      .left
      .map(exc =>
        ErrorMessage(
          "Problem parsing body from Server.",
          s"Message:${exc.getMessage}\nBody: ${req.responseText}"
        )
      )
}
