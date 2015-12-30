// requires
var util = require('util');
var qx = require("../target/qooxdoo-sdk/qooxdoo-sdk/tool/grunt");

// grunt
module.exports = function(grunt) {
  var config = {

    generator_config: {
      let: {
      }
    },

    common: {
      "APPLICATION" : "basic",
      "QOOXDOO_PATH" : "../target/qooxdoo-sdk/qooxdoo-sdk",
      "LOCALES": ["en"],
      "QXTHEME": "basic.theme.Theme"
    }

    /*
    myTask: {
      options: {},
      myTarget: {
        options: {}
      }
    }
    */
  };

  var mergedConf = qx.config.mergeConfig(config);
  // console.log(util.inspect(mergedConf, false, null));
  grunt.initConfig(mergedConf);

  qx.task.registerTasks(grunt);

  // grunt.loadNpmTasks('grunt-my-plugin');
};
