# Docker for Creating and Running the Unit Tests
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


