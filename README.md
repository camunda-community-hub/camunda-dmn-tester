[ ![Download](https://api.bintray.com/packages/pme123/maven/camunda-dmn-tester-server/images/download.svg) ](https://bintray.com/pme123/maven/camunda-dmn-tester-server/_latestVersion)
# Camunda DMN Table Tester

>A little DMN Table tester with the following Goals:
> * As a developer I want to test the DMNs that I get from the Business, even not knowing the concrete rules.
> * Business people can create their own tests.
> * They can easily adjust the tests to the dynamic nature of DMN Tables.

## Usage
I wrote a blog article that explains how you can use it:

[Testing (Camunda)-DMN Tables automatically](https://pme123.medium.com/testing-camunda-dmn-tables-automatically-713497ab57e6)

I plan to write another Blog to show how you can use the DMN Tester for continuous integration CI.

## Technologies
This projects builds on cool Open Source Projects. So my thanks go to:

### Shared
* [Autowire](https://github.com/lihaoyi/autowire):
  > Autowire is a pair of macros that allows you to perform type-safe, reflection-free RPC between Scala systems.
* [BooPickle](https://boopickle.suzaku.io):
  > BooPickle is the fastest and most size efficient serialization (aka pickling) library that works on both Scala and Scala.js.

### Client
* [Slinky](https://slinky.dev)
  > Write React apps in Scala just like you would in ES6
* [Scalably Typed](https://scalablytyped.org)
  > The Javascript ecosystem for Scala.js!
  I used the facades for Ant Design
* [Ant Design](https://ant.design)
  >A design system for enterprise-level products. Create an efficient and enjoyable work experience.

### Server
* [Scala DMN](https://github.com/camunda/dmn-scala)
  > An engine to execute decisions according to the DMN 1.1 specification.
* [http4s](https://http4s.org)
  > Typeful, functional, streaming HTTP for Scala.
* [ZIO Config](https://zio.github.io/zio-config/)
  > A functional, composable ZIO interface to configuration
* [ZIO](https://zio.dev)
  > Type-safe, composable asynchronous and concurrent programming for Scala

### Start Script
* [Ammonite](https://ammonite.io/#Ammonite)
  > Ammonite lets you use the Scala language for scripting purposes: in the REPL, as scripts, as a library to use in existing projects, or as a standalone systems shell.
## Development
### Server
`sbt server/run`

This starts the Web Server on **Port 8883**.

>This copies the client assets to the classpath of the server.
> So make sure you run `build` before.
>
> Or use the client as described in the next chapter.

### Client
`sbt dev`

This will watch all your changes in the client and automatically refresh your Browser Session.

Open in the Browser **http://localhost:8024**.

## Releasing
### Library
At the moment there are 2 steps:
1. Build the Client (full optimization) and the Server:

   `sbt release`
2. Publish: 
   
   `sbt publishLocal` or `sbt publish`

### Docker

`sbt server/docker:publishLocal` creates a Docker Image - see also next chapter.

## Try it out
There is a Docker Image you can use with the according Scripts in the `demo` directory.

`cd demo`

We support 2 Scenarios.

### Create DMN Table Tests
Use the following command:
```bash
docker run \
  --name camunda-dmn-tester \
   --rm \
   -e TESTER_CONFIG_PATHS:="/dmnConfigs" \
   -p 8883:8883 \
   -v $(pwd):/opt/workspace \
   -v $HOME/.ivy2:/root/.ivy2 \
   pame/camunda-dmn-tester:0.9.0-SNAPSHOT \
   ./amm testRunner.sc
```
Now open in your Browser: http://localhost:8883

### Run the Unit Tests
If you have configured your DMN tests, you can use the following Docker command to run them:

```bash
docker run \
  --name camunda-dmn-tester \
   --rm \
   -e TESTER_CONFIG_PATHS:="/dmnConfigs" \
   -v $(pwd):/opt/workspace \
   -v $HOME/.ivy2:/root/.ivy2 \
   pame/camunda-dmn-tester:0.9.0-SNAPSHOT \
   ./amm unitTestRunner.sc
```

This creates automatically Unit Tests of your DMN Tests and runs them with `sbt`.

In the end you will have the Test Reports (`target/test-reports`) you can show, for example in your CI-Tool.

### docker-compose
I provided a Docker Compose File that works for the Demo.

`docker-compose -f docker-compose.yml --project-directory . -p camunda-dmn-tester up`


