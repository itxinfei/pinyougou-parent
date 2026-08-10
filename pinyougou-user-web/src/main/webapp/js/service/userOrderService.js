//用户中心订单服务
app.service('userOrderService', function($http) {
    //查询订单列表（按状态筛选）
    this.search = function(page, rows, searchEntity) {
        return $http.post('../order/search.do?page=' + page + '&rows=' + rows, searchEntity);
    };

    //查询订单详情
    this.findOne = function(id) {
        return $http.get('../order/findOne.do?id=' + id);
    };

    //更新订单状态
    this.updateStatus = function(orderId, status) {
        return $http.get('../order/updateStatus.do?orderId=' + orderId + '&status=' + status);
    };

    //删除订单
    this.delete = function(ids) {
        return $http.get('../order/delete.do?ids=' + ids);
    };

    //获取当前登录用户
    this.getLoginName = function() {
        return $http.get('../login/getLoginName.do');
    };
});
