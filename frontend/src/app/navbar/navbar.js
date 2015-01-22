'use strict';

angular.module('techex')
    .controller('NavbarCtrl', ['$scope', '$location', function ($scope, $location) {

        /**
         * Determines whether the given route equals current location
         * @param route the route to check, e.g. '/badges'
         * @returns {string} returns 'active' if the route matches the current url, otherwise an empty string.
         */
        $scope.state = function(route) {
            return $location.url() === route ? 'active' : '';
        }
    }]);
