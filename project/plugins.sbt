addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.6.1")

resolvers += Resolver.bintrayRepo("oyvindberg", "converter")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta28")


addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")
