'use strict';

var gulp = require('gulp'),
    historyApiFallback = require('connect-history-api-fallback')

var plugin = require('gulp-load-plugins')({
    pattern: ['gulp-*', 'del', 'browser-sync']
});

var reload = plugin.browserSync.reload;


gulp.task('styles', function() {
    return gulp.src(['./src/app/vendor.scss', './src/app/app.scss'])
        .pipe(plugin.sass())
        .pipe(gulp.dest('./.tmp/styles/'))
});

gulp.task('html', function() {
    return gulp.src('./src/app/**/index.html')
        .pipe(gulp.dest('./.tmp'))
        .pipe(reload({stream:true}));
});

gulp.task('partials',  function () {
    return gulp.src(['./src/app/**/*.html', '!./src/app/index.html'])
        .pipe(plugin.minifyHtml({
            empty: true,
            spare: true,
            quotes: true
        }))
        .pipe(plugin.angularTemplatecache('templateCacheHtml.js', {
            module: 'techex'
        }))
        .pipe(gulp.dest('./.tmp/js'))
        .pipe(reload({stream:true}));
});

gulp.task('js', function() {
    return gulp.src('./src/app/**/*.js')
        .pipe(gulp.dest('./.tmp/js'))
        .pipe(reload({stream:true}));
});

gulp.task('assets', function() {
    return gulp.src('./src/assets/**/*')
        .pipe(gulp.dest('./.tmp'))
        .pipe(gulp.dest('./dist'))
        .pipe(reload({stream:true}));
});

gulp.task('source', ['html','js', 'partials', 'styles']);

gulp.task('usemin', ['source'], function () {
    return gulp.src('./.tmp/**/*.html')
        .pipe(plugin.usemin({
            //css: [plugin.minifyCss(), 'concat'],
            //html: [plugin.minifyHtml({empty: true})],
            //js: [plugin.sourcemaps.init(), plugin.uglify(), plugin.rev(), plugin.sourcemaps.write('.')]
        }))
        .pipe(gulp.dest('./dist'))
        .pipe(reload({stream:true}));
});


gulp.task('clean', function (done) {
    plugin.del(['./dist/', './.tmp/'], done);
});


gulp.task('browser-sync-dev', function() {
    plugin.browserSync({
        server: {
            baseDir: "./.tmp",
            routes: {
                "/bower_components": "./bower_components"
            },
            // See: https://github.com/shakyShane/browser-sync/issues/204
            middleware: [historyApiFallback]
        }
    });
});

gulp.task('browser-sync-dist', function() {
    plugin.browserSync({
        server: {
            baseDir: "./dist"
        },
        middleware: [historyApiFallback]
    });
});

gulp.task('serve', ['source', 'assets'], function () {
    gulp.start('browser-sync-dev');
    gulp.watch("./src/**/*", ['source']);
    gulp.watch("./src/assets/**/*", ['assets']);
});

gulp.task('serve:dist', ['default'], function() {
    gulp.start('browser-sync-dist');
    gulp.watch("./src/**/*", ['usemin']);
    gulp.watch("./src/assets/**/*", ['assets']);
});

gulp.task('default', ['clean'], function(){
    gulp.start(['usemin', 'assets']);
});

gulp.task('build', ['clean'], function() {
    gulp.start(['source', 'assets'])
});

