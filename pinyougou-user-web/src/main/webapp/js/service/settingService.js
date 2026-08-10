// 用户设置服务
app.service('settingService', function($http) {
    // 获取当前用户信息
    this.getUserInfo = function() {
        return $http.get('../user/getUserInfo.do');
    }
    // 更新用户信息
    this.updateUserInfo = function(entity) {
        return $http.post('../user/updateUserInfo.do', entity);
    }
    // 获取地址列表
    this.getAddressList = function() {
        return $http.get('../address/findListByLoginUser.do');
    }
    // 保存地址
    this.saveAddress = function(entity) {
        return $http.post('../address/add.do', entity);
    }
    // 删除地址
    this.deleteAddress = function(id) {
        return $http.get('../address/delete.do?ids=' + id);
    }
    // 设置默认地址
    this.setDefault = function(id) {
        return $http.get('../address/setDefault.do?id=' + id);
    }
    // 修改密码
    this.updatePassword = function(entity) {
        return $http.post('../user/updatePassword.do', entity);
    }
});
