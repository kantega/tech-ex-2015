'use strict';

angular.module('techex')
    .controller('HighscoreController', ['$scope', 'Highscore', function ($scope, Highscore) {
        console.log("HighscoreController initialized");
        $scope.scores = Highscore.query();
    }])

    .factory('Highscore', ['$resource', 'baseUrl', function($resource, baseUrl){

        return $resource(baseUrl + '/stats/badges', {}, {
            query: { method:'GET', isArray:true }
        })
    }])
;
