# HOW TO USE
 
## Install Ammonite 
> see https://ammonite.io/#Ammonite-REPL

* Add test configurations in `dmn-configs`

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

 * Run it in the terminal
 
`cd dmnTester`

`amm testRunner.sc`
