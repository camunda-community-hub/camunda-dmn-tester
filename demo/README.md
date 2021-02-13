## Docker 

```bash
docker run \
  --name camunda-dmn-tester \
   --rm \
   -e TESTER_CONFIG_PATHS:="/dmnConfigs" \
   -p 8883:8883 \
   -v $(pwd):/opt/workspace \
   -v $HOME/.ivy2:/root/.ivy2 \
   --entrypoint /bin/bash \
  pame/camunda-dmn-tester:0.9.0-SNAPSHOT \
  ./amm unitTestRunner.sc
```

