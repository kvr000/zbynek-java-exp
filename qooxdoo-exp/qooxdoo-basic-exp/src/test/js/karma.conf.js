module.exports = function(config) {
    config.set({
        basePath: '../../..',
        frameworks: ['jasmine'],
        files: [
            'src/main/js/**/*.js',
            'src/main/js/*.js',
            'src/test/js/**/*.js',
            'src/test/js/*.js'
        ],
        exclude: ['src/test/js/karma.conf*.js'],
        reporters: ['progress'],
        port: 9876,
        logLevel: config.LOG_INFO,
        browsers: ['PhantomJS'],
        singleRun: false,
        autoWatch: true,
        plugins: [
            'karma-jasmine',
            'karma-phantomjs-launcher'
        ]
    });
};
