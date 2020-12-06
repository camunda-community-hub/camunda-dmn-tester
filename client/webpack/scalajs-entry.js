if (process.env.NODE_ENV === "production") {
    const opt = require("./camunda-dmn-tester-client-opt.js");
    opt.main();
    module.exports = opt;
} else {
    var exports = window;
    exports.require = require("./camunda-dmn-tester-client-fastopt-entrypoint.js").require;
    window.global = window;

    const fastOpt = require("./camunda-dmn-tester-client-fastopt.js");
    fastOpt.main()
    module.exports = fastOpt;

    if (module.hot) {
        module.hot.accept();
    }
}
