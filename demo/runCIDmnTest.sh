docker run \
  --name camunda-dmn-tester-ci \
   --rm \
   -it \
   -e TESTER_CONFIG_PATHS="/dmnConfigs" \
   -v $(pwd)/dmnConfigs:/opt/workspace/dmnConfigs \
   -v $(pwd)/dmns:/opt/workspace/dmns \
   -v $(pwd)/target:/opt/workspace/target \
   -v $HOME/.ivy2:/root/.ivy2 \
   pame/camunda-dmn-tester-ci:0.18.0-SNAPSHOT
