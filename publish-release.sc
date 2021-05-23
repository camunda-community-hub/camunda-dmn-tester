import ammonite.ops._


/** <pre>
  * Creates a new Release for the client and publishes to the Artifactory:
  *
  * amm ./publish-release.sc <VERSION>
  *
  * # Example SNAPSHOT (only publish to SNAPSHOT Repo, e.g. bpfpkg-maven-dev)
  * amm ./publish-release.sc 0.2.5-SNAPSHOT
  *
  * # Example (publish to Release Repo (e.g. bpfpkg-maven-release) and GIT Tagging and increasing Version to next minor Version)
  * amm ./publish-release.sc 0.2.5
  */

private implicit val workDir: Path = {
  val wd = pwd
  println(s"Working Directory: $wd")
  wd
}

private def replaceVersion(version: String) = {
  val pattern = """^(\d+)\.(\d+)\.(\d+)$""".r

  val newVersion = version match {
    case pattern(major, minor, _) =>
      s"$major.${minor.toInt + 1}.0-SNAPSHOT"
  }
  write.over(pwd / "version", newVersion)
  newVersion
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
    %.sbt(
      "-mem",
      "3000",
      "release",
      "publish",
      "server/docker:publish"
    )
    %.git(
      "fetch",
      "--all"
    )
    %.git(
      "commit",
      "-a",
      "-m",
      s"Released Version $version"
    )
    %.git(
      "tag",
      "-a",
      version,
      "-m",
      s"Version $version"
    )
    %.git(
      "push"
    )
    %.git(
      "checkout",
      "master"
    )
    %.git(
      "merge",
      "develop"
    )
    %.git(
      "push"
    )
    %.git(
      "checkout",
      "develop"
    )
    val newVersion = replaceVersion(version)
    %.git(
      "commit",
      "-a",
      "-m",
      s"Init new Version $newVersion"
    )
    %.git(
      "push"
    )
  } else {
    %.sbt(
      "-mem",
      "3000",
      "release",
      "publishLocal",
      "server/docker:publishLocal"
    )
  }
}
