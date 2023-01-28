package pme123.camunda.dmn.tester.server

import cats.effect._
import com.comcast.ip4s._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.UrlForm.entityEncoder
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, Server}
import org.slf4j.{Logger, LoggerFactory}
import org.typelevel.ci._
import pme123.camunda.dmn.tester.shared._

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object HttpServer extends IOApp {

  val logger: Logger = LoggerFactory.getLogger(getClass)

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
      "/api" -> apiServices
    ).orNotFound

  private object ConfigPathQueryParamMatcher
      extends QueryParamDecoderMatcher[String]("path")

  private lazy val apiServices = HttpRoutes.of[IO] {
    case GET -> Root / "basePath" =>
      Ok(DmnService.getBasePath())
    case GET -> Root / "configPaths" =>
      Ok(DmnService.getConfigPaths().asJson)
    case GET -> Root / "dmnConfigs" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      val configs =
        if (decConfigPath.isBlank)
          Seq.empty
        else
          DmnService
            .getConfigs(decConfigPath.split("/"))
      Ok(configs.asJson)
    case GET -> Root / "validateDmnPath" :? ConfigPathQueryParamMatcher(
          dmnPath
        ) =>
      val decDmnPath = URLDecoder.decode(dmnPath, StandardCharsets.UTF_8)
      val pathExists = DmnService
            .dmnPathExists(decDmnPath)
      Ok(pathExists)

    case req @ POST -> Root / "runDmnTests" =>
      Ok(
        req
          .as[Option[Seq[DmnConfig]]]
          .map(configs =>
            if (configs.nonEmpty && configs.get.nonEmpty) {
              val result = DmnService.runTests(configs.get).asJson
              result
            } else
              Seq.empty.asJson
          )
      )
    case req @ PUT -> Root / "dmnConfig" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      println(s"update DMN Config: started: ")
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Ok(
        req
          .as[DmnConfig]
          .map(config => {
            DmnService
              .updateConfig(config, decConfigPath.split("/"))
          })
          .map(_.asJson)
      )
    case req @ DELETE -> Root / "dmnConfig" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      println(s"update DMN Config: started: ")
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Ok(
        req
          .as[DmnConfig]
          .map(config => {
            DmnService
              .deleteConfig(config, decConfigPath.split("/"))
          })
          .map(_.asJson)
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
