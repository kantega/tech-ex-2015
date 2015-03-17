'use strict';

angular.module('techex', ['ngRoute', 'ngResource', 'ui.bootstrap'])

    .constant('baseUrl', 'https://kantegex.kantega.no')

    .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
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

        // use the HTML5 History API
        $locationProvider.html5Mode(true);
    }]);