'use strict';

angular.module('techex')
    .controller('StatsController', ['$scope', 'Progress', function ($scope, Progress) {
        $scope.progress = Progress.query()

    }])
    .factory('Progress', ['$resource', 'baseUrl', function($resource, baseUrl){

        return $resource(baseUrl + '/stats/progress', {}, {
            query: { method:'GET', isArray:true }
        })
    }])
;