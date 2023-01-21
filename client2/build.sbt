import org.scalajs.linker.interface.ModuleSplitStyle

lazy val camundaDmnTester = project.in(file("."))
  .enablePlugins(ScalaJSPlugin) // Enable the Scala.js plugin in this project
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    scalaVersion := "3.2.1",

    // Tell Scala.js that this is an application with a main method
    scalaJSUseMainModuleInitializer := true,

    /* Configure Scala.js to emit modules in the optimal way to
     * connect to Vite's incremental reload.
     * - emit ECMAScript modules
     * - emit as many small modules as possible for classes in the "camunda-dmn-tester" package
     * - emit as few (large) modules as possible for all other classes
     *   (in particular, for the standard library)
     */
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("camunda-dmn-tester")))
    },
    scalacOptions ++= Seq(
      "-Xmax-inlines",
      "100" // is declared as erased, but is in fact used
    ),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.2.0",
      "be.doeraene" %%% "web-components-ui5" % "1.9.0",
      "com.raquo" %%% "laminar" % "0.14.5",
      // serializing
      "io.suzaku" %%% "boopickle" % "1.4.0",
      "io.circe" %%% "circe-generic" % "0.14.3",    
      "io.circe" %%% "circe-parser" % "0.14.3"
    ),

    // Tell ScalablyTyped that we manage `npm install` ourselves
    externalNpm := baseDirectory.value
  )
