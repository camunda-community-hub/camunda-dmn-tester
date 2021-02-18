# Docker Commands
Examples on how to run the App and Unit Tests with Docker or Docker-Compose.
## DMN Tester App
Starts the Webserver with the DMN Tester App.

Open http://localhost:8883 in your Browser.
### Docker Run
```
docker run \
  --name camunda-dmn-tester \
   --rm \
   -it \
   -e TESTER_CONFIG_PATHS="/dmnConfigs" \
   -v $(pwd)/dmns:/opt/docker/dmns \
   -v $(pwd)/dmnConfigs:/opt/docker/dmnConfigs \
   -p 8883:8883 \
   pame/camunda-dmn-tester
```
### Docker Compose
```
docker-compose -f docker-compose-ci.yml --project-directory . -p camunda-dmn-tester-ci up
```

## Running the DMN Unit Tests
This creates automatically Unit Tests of your DMN Tests and runs them with `sbt`.

In the end you will have the Test Reports (`target/test-reports`) you can show, for example in your CI-Tool.

### Docker Run
```
docker run \
  --name camunda-dmn-tester-ci \
   --rm \
   -it \
   -e TESTER_CONFIG_PATHS="/dmnConfigs" \
   -v $(pwd)/dmnConfigs:/opt/workspace/dmnConfigs \
   -v $(pwd)/target:/opt/workspace/target \
   -v $HOME/.ivy2:/root/.ivy2 \
   pame/camunda-dmn-tester-ci
```
### Docker Compose
```
docker-compose -f docker-compose-ci.yml --project-directory . -p camunda-dmn-tester-ci up
```

