//用户中心订单控制器
app.controller('userOrderController', function($scope, $timeout, $location, userOrderService) {

    //分页配置
    $scope.paginationConf = {
        currentPage: 1,
        itemsPerPage: 10,
        totalItems: 0
    };

    //当前订单状态（由页面初始化时设置）
    $scope.orderStatus = '';

    //状态映射
    $scope.statusMap = {
        '1': '待付款',
        '2': '已付款',
        '3': '待发货',
        '4': '已发货',
        '5': '交易成功',
        '6': '交易关闭'
    };

    //初始化
    $scope.init = function(status) {
        $scope.orderStatus = status;
        $scope.search(1, $scope.paginationConf.itemsPerPage);
    };

    //搜索订单
    $scope.search = function(page, rows) {
        var searchEntity = {
            status: $scope.orderStatus
        };
        userOrderService.search(page, rows, searchEntity).success(
            function(response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;
                $scope.paginationConf.currentPage = page;
            }
        ).error(function() {
            $scope.list = [];
            $scope.showMessage('error', '获取订单列表失败');
        });
    };

    //刷新列表
    $scope.reloadList = function() {
        $scope.search($scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
    };

    //翻页
    $scope.goToPage = function(page) {
        var totalPages = Math.ceil($scope.paginationConf.totalItems / $scope.paginationConf.itemsPerPage);
        if (page >= 1 && page <= totalPages) {
            $scope.search(page, $scope.paginationConf.itemsPerPage);
        }
    };

    //获取状态名称
    $scope.getStatusName = function(status) {
        return $scope.statusMap[status] || '未知状态';
    };

    //格式化日期
    $scope.formatDate = function(dateStr) {
        if (!dateStr) return '';
        var date = new Date(dateStr);
        var year = date.getFullYear();
        var month = ('0' + (date.getMonth() + 1)).slice(-2);
        var day = ('0' + date.getDate()).slice(-2);
        var hours = ('0' + date.getHours()).slice(-2);
        var minutes = ('0' + date.getMinutes()).slice(-2);
        return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
    };

    //计算订单总金额
    $scope.calcTotal = function(order) {
        if (!order || !order.orderItemList) return 0;
        var total = 0;
        for (var i = 0; i < order.orderItemList.length; i++) {
            total += order.orderItemList[i].totalFee || 0;
        }
        return total;
    };

    //取消订单
    $scope.cancelOrder = function(orderId) {
        if (!confirm('确定要取消该订单吗？')) return;
        userOrderService.updateStatus(orderId, '6').success(
            function(response) {
                if (response.success) {
                    $scope.showMessage('success', '订单已取消');
                    $scope.reloadList();
                } else {
                    $scope.showMessage('error', response.message || '取消失败');
                }
            }
        ).error(function() {
            $scope.showMessage('error', '网络请求失败');
        });
    };

    //确认收货
    $scope.confirmReceive = function(orderId) {
        if (!confirm('确认已收到货物？')) return;
        userOrderService.updateStatus(orderId, '5').success(
            function(response) {
                if (response.success) {
                    $scope.showMessage('success', '已确认收货');
                    $scope.reloadList();
                } else {
                    $scope.showMessage('error', response.message || '确认失败');
                }
            }
        ).error(function() {
            $scope.showMessage('error', '网络请求失败');
        });
    };

    //删除订单
    $scope.deleteOrder = function(orderId) {
        if (!confirm('确定要删除该订单吗？')) return;
        userOrderService.delete(orderId).success(
            function(response) {
                if (response.success) {
                    $scope.showMessage('success', '订单已删除');
                    $scope.reloadList();
                } else {
                    $scope.showMessage('error', response.message || '删除失败');
                }
            }
        ).error(function() {
            $scope.showMessage('error', '网络请求失败');
        });
    };

    //去付款
    $scope.goToPay = function(orderId) {
        location.href = '../cart/pay.html?orderId=' + orderId;
    };

    //去评价
    $scope.goToEvaluate = function(orderId) {
        location.href = 'home-order-evaluate.html?orderId=' + orderId;
    };

    //消息提示
    $scope.showMessage = function(type, msg, duration) {
        $scope.messageType = type;
        $scope.message = msg;
        // 使用 $timeout 替代 setTimeout，确保 AngularJS digest cycle 正确执行
        $timeout(function() {
            $scope.message = '';
            $scope.messageType = '';
        }, duration || 3000);
    };
});
