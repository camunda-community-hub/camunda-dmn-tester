import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt.{Level, _}

import scala.io.Source
import scala.util.Using

object Settings {

  val scala2V = "2.13.12"
  val scala3V = "3.2.2"

  val projectName = "camunda-dmn-tester"
  lazy val projectVersion: String =
    Using(Source.fromFile("version"))(_.mkString.trim).get

  lazy val projectSettings: Project => Project =
    _.settings(
      organization := "io.github.pme123",
      version := projectVersion
    )

  lazy val ReleaseCmd = Command.command("release") { state =>
    "server/compile" ::
      "server/stage" ::
      state
  }

  lazy val ReleaseClientCmd = Command.command("releaseClient") { state =>
    "clean" ::
      "fullLinkJS" ::
      state
  }

  lazy val publicationSettings: Project => Project = _.settings(
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/pme123/camunda-dmn-tester")),
    startYear := Some(2020),
    logLevel := Level.Debug,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/pme123/camunda-dmn-tester"),
        "scm:git:github.com:/pme123/camunda-dmn-tester"
      )
    ),
    developers := List(
      Developer(
        id = "pme123",
        name = "Pascal Mengelt",
        email = "pascal.mengelt@gmail.com",
        url = url("https://github.com/pme123")
      )
    )
  )

  lazy val preventPublication: Project => Project =
    _.settings(
      publish := {},
      publishTo := Some(
        Resolver
          .file("Unused transient repository", target.value / "fakepublish")
      ),
      publishArtifact := false,
      publishLocal := {},
      packagedArtifacts := Map.empty
    ) // doesn't work - https://github.com/sbt/sbt-pgp/issues/42

  object server {
    lazy val settings: Project => Project = _.settings(
      name := s"$projectName-server",
      scalaVersion := scala2V,
      Compile / unmanagedResourceDirectories += baseDirectory.value / "../client/dist",
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      resolvers += Resolver.mavenLocal, // only needed for dmn-engine SNAPSHOT
      Test / unmanagedSourceDirectories += baseDirectory.value / "target" / "generated-src"
    )

    lazy val serverDeps: Project => Project =
      _.settings(
        libraryDependencies ++= Seq(
          Deps.http4sDsl,
          Deps.http4sServer,
          Deps.logback
        )
      )

    lazy val deps: Project => Project = _.settings(
      libraryDependencies ++= Seq(
        Deps.ammonite,
        Deps.dmnScala,
        // Optional for auto-derivation of JSON codecs
        "org.http4s" %% "http4s-circe" % Deps.version.http4s,
        Deps.zio,
        Deps.zioCats,
        Deps.zioConfigHocon,
        Deps.zioConfigMagnolia,
        Deps.zioTest % Test,
        Deps.zioTestSbt % Test,
        Deps.scalaTest % Test
      )
    )

    lazy val docker: Project => Project =
      _.settings(
        dockerBaseImage := "openjdk:11", //eed3si9n/sbt:jdk11-alpine",
        dockerExposedPorts ++= Seq(8883),
        Docker / packageName := projectName,
        dockerUsername := Some("pame"),
        dockerUpdateLatest := true
      ).enablePlugins(DockerPlugin)
  }

  object client {
    lazy val settings: Project => Project = _.settings(
      name := s"$projectName-client",
      scalaVersion := scala3V,
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.ESModule)
          .withModuleSplitStyle(
            ModuleSplitStyle.SmallModulesFor(List("camunda-dmn-tester"))
          )
      },
      scalacOptions ++= Seq(
        "-Xmax-inlines",
        "100" // is declared as erased, but is in fact used
      )
    )

    lazy val deps: Project => Project = _.settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.2.0",
        "be.doeraene" %%% "web-components-ui5" % "1.9.0",
        "com.raquo" %%% "laminar" % "0.14.5"
      )
    )
  }
}
