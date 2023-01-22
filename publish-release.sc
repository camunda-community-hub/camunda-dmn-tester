#!/usr/bin/env amm

import mainargs._

/** <pre>
  * Creates a new Release for the DmnTester (Library and Docker) and the camunda-dmn-tester-ci (Docker):
  *
  * amm ./publish-release.sc <VERSION>
  *
  * # Example SNAPSHOT (only publish to SNAPSHOT Repo, e.g. bpfpkg-maven-dev)
  * amm ./publish-release.sc 0.2.5-SNAPSHOT
  *
  * # Example (publish to Release Repo (e.g. bpfpkg-maven-release) and GIT Tagging and increasing Version to next minor Version)
  * amm ./publish-release.sc 0.2.5
  */

private implicit val workDir: os.Path = {
  val wd = os.pwd
  println(s"Working Directory: $wd")
  wd
}

private def replaceVersion(version: String) = {
  val pattern = """^(\d+)\.(\d+)\.(\d+)$""".r

  val newVersion = version match {
    case pattern(major, minor, _) =>
      s"$major.${minor.toInt + 1}.0-SNAPSHOT"
  }
  os.write.over(os.pwd / "version", newVersion)
  newVersion
}

private def publishCIDocker(version: String) = {
  os.write.over(os.pwd / "docker" / "data" / "testerVersion", version)
  runAndPrint( "docker",
    "build",
    os.pwd / "docker",
    "-t",
    s"pame/camunda-dmn-tester-ci:$version"
  )
  runAndPrint( "docker",
    "push",
    s"pame/camunda-dmn-tester-ci:$version"
  )
}

private def updateGit(version: String) = {
  runAndPrint( "git",
    "fetch",
    "--all"
  )
  runAndPrint( "git",
    "commit",
    "-a",
    "-m",
    s"Released Version $version"
  )
  runAndPrint( "git",
    "tag",
    "-a",
    version,
    "-m",
    s"Version $version"
  )
  runAndPrint( "git",
    "push",
    "--set-upstream",
    "origin",
    "develop"
  )
  runAndPrint( "git",
    "checkout",
    "master"
  )
  runAndPrint( "git",
    "merge",
    "develop"
  )
  runAndPrint( "git",
    "push",
    "--tags"
  )
  runAndPrint( "git",
    "checkout",
    "develop"
  )
  val newVersion = replaceVersion(version)
  runAndPrint( "git",
    "commit",
    "-a",
    "-m",
    s"Init new Version $newVersion"
  )
  runAndPrint( "git",
    "push"
  )
}
def publishTesterDocker(version: String) = {
  os.write.over(os.pwd / "version", version)
  buildClient
  runAndPrint(
    "sbt",
    "-J-Xmx3G",
    "release",
    "publishSigned",
    "server/docker:publish"
  )
}

def publishTesterDockerLocal = {
  buildClient
  runAndPrint(
    "sbt",
    "-J-Xmx3G",
    "release",
    "publishLocal",
    "server/docker:publishLocal"
  )
}

def buildClient = {
  runAndPrint(
    "sbt",
    "-J-Xmx3G",
    "releaseClient",
  )
  runAndPrint(
    "npm",
    "run",
    "build",
  )
}

def runAndPrint(commands: os.Shellable*) = {
  println(s"Commands: ${commands.mkString(", ")}")
  println(os.proc(commands:_*).call())
}

@arg(
  doc =
    "> Creates a new Release for the package and publishes to bpf-generic-release"
)
@main
def release(version: String): Unit = {
  println(s"Publishing BPF Package: $version")

  val releaseVersion = """^(\d+)\.(\d+)\.(\d+)(-.*)?$"""
  if (!version.matches(releaseVersion))
    throw new IllegalArgumentException(
      "Your Version has not the expected format (2.1.2(-SNAPSHOT))"
    )

  val isSnapshot = version.contains("-")
  if (!isSnapshot) {
    publishTesterDocker(version)
    publishCIDocker(version)
    updateGit(version)
    println("""Due to problems with the `"org.xerial.sbt" % "sbt-sonatype"` Plugin you have to release manually:
              |- https://s01.oss.sonatype.org/#stagingRepositories
              |  - login
              |  - check Staging Repository
              |  - hit _close_ Button
              |  - hit _release_ Button""".stripMargin)
  } else {
    publishTesterDockerLocal
  }

}
