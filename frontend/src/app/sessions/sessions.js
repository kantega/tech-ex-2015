'use strict';

angular.module('techex')
    .controller('SessionsController', ['$scope', 'SessionService', function ($scope, SessionService) {

        SessionService.all().then(function(sessions){
            $scope.sessions = sessions;
        });

        $scope.start = function(sessionId) {
            $scope.sessions.forEach(function(s){
                if (s.id == sessionId) {
                    s.started = true;
                    SessionService.start(sessionId);
                }
            });
            console.log("Starting session " + sessionId);
        };

        $scope.end = function(sessionId) {
            console.log("Ending session " + sessionId);
            $scope.sessions.forEach(function(s){
                if (s.id == sessionId) {
                    s.ended = true;
                    SessionService.end(sessionId);
                }
            });
        };
    }])

    .factory('SessionService', ['$http', '$q', 'baseUrl', function($http, $q, baseUrl){

        var all = function() {
            return $http.get(baseUrl + '/sessions').then(handleSuccess, handleError);
        };

        var start = function(sessionId) {
            return $http.post(baseUrl + '/sessions/start/' + sessionId, {}).then(handleSuccess, handleError);
        };

        var end = function(sessionId) {
            return $http.post(baseUrl + '/sessions/end/' + sessionId, {}).then(handleSuccess, handleError);
        };

        // Transform the successful response, unwrapping the application data
        // from the API response payload.
        var handleSuccess = function(response) {
            return(response.data);
        };

        var handleError = function( response ) {
            if (! angular.isObject( response.data ) ||! response.data.message) {
                return( $q.reject( "An unknown error occurred." ) );
            }
            return( $q.reject( response.data.message ) );
        };

        // Public API
        return {
            all: all,
            start: start,
            end: end
        };

    }])

    .factory('Session', ['$http', function($http){
        return $resource('https://techex.kantega.no/session')
    }])

    .filter('minutes', [function(){
        return function(seconds) {
            return +seconds / 60 / 1000 + " min"
        }
    }])

;
