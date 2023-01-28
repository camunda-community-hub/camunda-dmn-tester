docker run \
  --name camunda-dmn-tester \
   --rm \
   -it \
   -e TESTER_CONFIG_PATHS="/dmnConfigs,/server/src/test/resources/dmn-configs" \
   -v $(pwd)/dmns:/opt/docker/dmns \
   -v $(pwd)/dmnConfigs:/opt/docker/dmnConfigs \
   -v $(pwd)/../server/src/test/resources:/opt/docker/server/src/test/resources \
   -p 8885:8883 \
   pame/camunda-dmn-tester:0.17.0-SNAPSHOT