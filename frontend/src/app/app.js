'use strict';

angular.module('techex', ['ngRoute', 'ngResource', 'ui.bootstrap'])

    .constant('baseUrl', 'https://techex.kantega.no')

    .config(['$routeProvider', '$locationProvider', '$httpProvider', function ($routeProvider, $locationProvider, $httpProvider) {
        $routeProvider
            .when('/', {
                templateUrl: 'infoscreen/infoscreen.html'
            })
            .when('/sessions', {
                templateUrl: 'sessions/sessions.html',
                controller: 'SessionsController'
            })
            .when('/stats', {
                templateUrl: 'stats/stats.html',
                controller: 'StatsController'
            })
            .otherwise({
                redirectTo: '/'
            });

        // Don't the HTML5 History API, as the server it runs on will f..ck it up.
        $locationProvider.html5Mode(false);

        // Prevent CORS on post.
        $httpProvider.defaults.headers.post = {
            'Content-Type': 'text/plain'
        };
    }]);