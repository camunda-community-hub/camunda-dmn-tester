# Docker for Creating and Running the Unit Tests

## With publish.release.sc
The following steps are done together if you run.

`amm ./publish-release.sc [VERSION]` (in Project dir!)


## Adjust Version
Adjust the version in `data/testerVersion`.
## Build Docker File
```
docker build . -t pame/camunda-dmn-tester-ci:{{versiom}}
```
## Push Docker Image
```
docker push pame/camunda-dmn-tester-ci:{{versiom}}
```


