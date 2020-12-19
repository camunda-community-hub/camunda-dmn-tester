# HOW TO USE
 
## Install Ammonite 
> see https://ammonite.io/#Ammonite-REPL

Copy this directory (`dmnTester`) in your Root directory of your project.

## Configuration
Add test configurations in `dmn-configs`.

Example `country-risk.conf`:
```
decisionId: country-risk,
dmnPath: [core, src, test, resources, country-risk.dmn],
data: {
  inputs: [{
    key: currentCountry,
    values: [CH, ch, DE, OTHER, an awful long Input that should be cutted]
  }, {
    key: targetCountry,
    values: [CH, ch, DE, OTHER, an awful long Input that should be cutted]
  }]
}
```
## Run it
In the terminal:

`amm testRunner.sc`

See [testRunner.sc](../testRunner.sc)
