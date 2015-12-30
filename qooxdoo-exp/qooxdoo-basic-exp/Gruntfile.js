module.exports = function(grunt) {
	grunt.loadNpmTasks('grunt-contrib-jshint');
	grunt.loadNpmTasks('grunt-typescript');
	grunt.loadNpmTasks('grunt-ts');

	grunt.initConfig({
		jshint: {
			files: ['Gruntfile.js', 'src/main/js/**/*.js','src/test/js/**/*.js']
		},

		typescript: {
			base: {
				src: ['src/main/typescript/**/*.ts'],
				dest: 'src/main/js/',
				options: {
					module: 'amd', //or commonjs 
					target: 'es5', //or es3 
					//basePath: 'src/main/typescript',
					sourceMap: true,
					declaration: true
				}
			}
		},

		ts: {
			base: {
				src: [ 'src/main/typescript/**/*.ts' ],
				dest: 'target/tsjs/',
				options: {
					baseDir: 'src/main/typescript/',
					outDir: "target/tsjs/",
					rootDir: "src/main/typescript/",
					module: 'amd',
					target: 'es5',
					sourceMap: true,
					declaration: true
				}
			}
		},
	});
	grunt.registerTask('default', [ 'jshint' ]);
	grunt.registerTask('compile', [ 'ts' ]);
};
