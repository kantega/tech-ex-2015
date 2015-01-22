'use strict';

angular.module('techex')
    .controller('QuestsCtrl', ['$scope', 'Quests', function ($scope, Quests) {
        $scope.quests = Quests.query();
        $scope.first = Quests.get({questId: 1})
    }])

    .factory('Quests', ['$resource', function($resource){

        return $resource('http://private-c877-techex.apiary-mock.com/quests/:questId', {}, {
            query: { method:'GET', isArray:true }
        })
    }])
;
