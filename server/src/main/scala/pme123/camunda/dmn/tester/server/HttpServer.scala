package pme123.camunda.dmn.tester.server

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, Server}
import org.http4s.{EntityDecoder, _}
import org.slf4j.{Logger, LoggerFactory}
import pme123.camunda.dmn.tester.shared._

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success, Try}

object HttpServer extends IOApp {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  override def run(args: List[String]): IO[ExitCode] =
    IO(logger.info("Server starting at Port 8883")) *>
      app.use(_ => IO.never).as(ExitCode.Success)

  private val app: Resource[IO, Server] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8883")
      .withHttpApp(httpApp)
      .build
  }

  private lazy val httpApp =
    Router(
      "/" -> CORS.policy.withAllowOriginAll
        .withAllowMethodsIn(Set(Method.GET))
        .withAllowCredentials(false)
        .apply(guiServices),
      "/api" -> apiServices,
      "/info" -> infoService
    ).orNotFound

  private object ConfigPathQueryParamMatcher
      extends QueryParamDecoderMatcher[String]("path")

  private lazy val apiServices = HttpRoutes.of[IO] {
    case GET -> Root / "basePath" =>
      Try(DmnService.getBasePath) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }
    case GET -> Root / "configPaths" =>
      Try(DmnService.getConfigPaths.asJson) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }
    case GET -> Root / "dmnConfigs" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Try(
        if (decConfigPath.isBlank)
          Seq.empty
        else
          DmnService
            .getConfigs(decConfigPath.split("/"))
      ) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }
    case GET -> Root / "validateDmnPath" :? ConfigPathQueryParamMatcher(
          dmnPath
        ) =>
      val decDmnPath = URLDecoder.decode(dmnPath, StandardCharsets.UTF_8)
      Try(
        DmnService
          .dmnPathExists(decDmnPath)
      ) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }

    case req @ POST -> Root / "runDmnTests" =>

      Try(
        req
          .as[Option[Seq[DmnConfig]]]
          .map(configs =>
            if (configs.nonEmpty && configs.get.nonEmpty) {
              val testResult = DmnService.runTests(configs.get)
              // println(s"TEST RESULT: $testResult")
              val jsonTestResult = testResult.asJson
              println(s"JSON TEST RESULT: $jsonTestResult")
              jsonTestResult
            } else
              Seq.empty.asJson
          ).handleError{
          exception =>
            exception.printStackTrace()
            exception.getMessage.asJson
          }
      ) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }
    case req @ PUT -> Root / "dmnConfig" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Try(
        req
          .as[DmnConfig]
          .map(config => {
            DmnService
              .updateConfig(config, decConfigPath.split("/"))
          })
          .map(_.asJson)
      ) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }
    case req @ DELETE -> Root / "dmnConfig" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Try(
        req
          .as[DmnConfig]
          .map(config => {
            DmnService
              .deleteConfig(config, decConfigPath.split("/"))
          })
          .map(_.asJson)
      ) match {
        case Failure(exception) =>
          exception.printStackTrace()
          InternalServerError(exception.getMessage)
        case Success(value) =>
          Ok.apply(value)
      }
  }

  private lazy val infoService = HttpRoutes.of[IO] { case GET -> Root =>
    Ok.apply(
      (sys.props.get(STARTING_APP) orElse
        sys.env.get(STARTING_APP)).getOrElse(
        "Undefined Starting App. Please define STARTING_APP environment variable."
      )
    )
  }

  private lazy val guiServices = HttpRoutes.of[IO] {
    case req if req.method == Method.OPTIONS =>
      IO(
        Response(
          Ok,
          headers = Headers(Header.Raw(ci"Allow", "OPTIONS, GET, POST"))
        )
      )
    case request
        if request.uri.path.segments.headOption
          .contains(Uri.Path.Segment("testReports")) =>
      testReports(request)
    case request @ GET -> Root =>
      static("index.html", request)
    case request @ GET -> path =>
      static(path.toString, request)
  }

  private val testReportsPath: os.Path = os.pwd / "target" / "test-reports"

  private def static(file: String, request: Request[IO]) = {
    logger.info(s"Static File: $file")
    StaticFile
      .fromResource("/" + file, Some(request))
      .getOrElseF(NotFound())
  }

  private def testReports(request: Request[IO]) = {
    logger.info(s"TestReports path: $testReportsPath")
    StaticFile
      .fromString(
        (testReportsPath / "index.html").toIO.getAbsolutePath,
        Some(request)
      )
      .getOrElseF(NotFound())
  }

  implicit lazy val decoderConfig: EntityDecoder[IO, DmnConfig] =
    jsonOf[IO, DmnConfig]
  implicit lazy val decoderOptConfigs
      : EntityDecoder[IO, Option[Seq[DmnConfig]]] =
    jsonOf[IO, Option[Seq[DmnConfig]]]
  implicit lazy val decoderConfigs: EntityDecoder[IO, Seq[DmnConfig]] =
    jsonOf[IO, Seq[DmnConfig]]

}
