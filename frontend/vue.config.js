const path = require('path');

module.exports = {
    chainWebpack: config => {
        // Vue CLI typically names the plugin 'fork-ts-checker'
        const forkTsCheckerPlugin = config.plugins.get('fork-ts-checker');

        if (forkTsCheckerPlugin) {
            forkTsCheckerPlugin.tap(args => {
                // args[0] is the plugin options object
                if (!args[0].typescript) {
                    args[0].typescript = {};
                }
                // Explicitly set the configFile path
                args[0].typescript.configFile = path.resolve(__dirname, 'tsconfig.json');

                // Optionally, you can also ensure the context is correct,
                // though Vue CLI usually handles this. __dirname is the project root.
                // args[0].typescript.context = path.resolve(__dirname);

                return args;
            });
        }
    },
    pluginOptions: {
        electronBuilder: {
            mainProcessFile: 'background.js',
            // Preload file
            preload: 'preload.js',
            // Other electron-builder options
            builderOptions: {
                // Options used by electron-builder
                appId: 'com.pureapps.appread',
                productName: 'AppRead',
                // Add any other electron-builder options here
            }
        }
    }
};
