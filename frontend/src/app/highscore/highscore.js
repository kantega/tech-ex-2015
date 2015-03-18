'use strict';

angular.module('techex')
    .controller('HighscoreController', ['$scope', '$interval', 'Highscore', function ($scope, $interval, Highscore) {
        console.log("HighscoreController initialized");
        $scope.scores = Highscore.query();
        var poll = $interval(function(){
            Highscore.query(function(newScores){
                for (var i = 0; i < newScores.length; i++) {
                    $scope.scores[i] = newScores[i];
                }
            });
        }, 100000);


        $scope.$on("$destroy", function(){
            $interval.cancel(poll);
        });
    }])

    .factory('Highscore', ['$resource', 'baseUrl', function($resource, baseUrl){

        return $resource(baseUrl + '/stats/badges', {}, {
            query: { method:'GET', isArray:true }
        })
    }])
;
