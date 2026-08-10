// 订单详情控制器
app.controller('detailController', function($scope, $timeout, $location, loginService, orderService) {
    $scope.showName = function() {
        loginService.showName().success(function(response) {
            $scope.loginName = response.loginName;
        });
    }

    $scope.order = {};

    $scope.loadOrderDetail = function() {
        var orderId = $location.search()['orderId'];
        if (orderId) {
            orderService.findOne(orderId).success(function(response) {
                $scope.order = response;
                $scope.orderItemList = response.orderItems || [];
            });
        }
    }

    $scope.getStatusName = function(status) {
        var map = {'1':'未付款','2':'已付款','3':'待发货','4':'已发货','5':'交易成功','6':'交易关闭'};
        return map[status] || '未知状态';
    }

    $scope.showMessage = function(type, msg, duration) {
        $scope.messageType = type;
        $scope.message = msg;
        $timeout(function() {
            $scope.message = '';
            $scope.messageType = '';
        }, duration || 3000);
    };
});
