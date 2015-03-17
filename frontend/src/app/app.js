'use strict';

angular.module('techex', ['ngRoute', 'ngResource', 'ui.bootstrap'])

    .constant('baseUrl', 'https://kantegex.kantega.no')

    .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
        $routeProvider
            .when('/highscore', {
                templateUrl: 'highscore/highscore.html',
                controller: 'HighscoreController'
            })
            .when('/sessions', {
                templateUrl: 'sessions/sessions.html',
                controller: 'SessionsController'
            })
            .otherwise({
                redirectTo: '/highscore'
            });

        // use the HTML5 History API
        $locationProvider.html5Mode(true);
    }]);