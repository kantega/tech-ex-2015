'use strict';

angular.module('techex', ['ngRoute', 'ngResource'])
    .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
        $routeProvider
            .when('/', {
                templateUrl: 'quests/quests.html',
                controller: 'QuestsCtrl'
            })
            .when('/badges', {
                templateUrl: 'badges/badges.html',
                controller: 'BadgesCtrl'
            })
            .otherwise({
                redirectTo: '/'
            });

        // use the HTML5 History API
        $locationProvider.html5Mode(true);
    }]);