docker run \
  --name camunda-dmn-tester \
   --rm \
   -it \
   -e TESTER_CONFIG_PATHS="/dmnConfigs" \
   -v $(pwd)/dmns:/opt/docker/dmns \
   -v $(pwd)/dmnConfigs:/opt/docker/dmnConfigs \
   -p 8883:8883 \
   pame/camunda-dmn-tester