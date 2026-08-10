// 用户设置控制器
app.controller('settingController', function($scope, $timeout, $interval, loginService, settingService) {
    // 显示用户名
    $scope.showName = function() {
        loginService.showName().success(function(response) {
            $scope.loginName = response.loginName;
        });
    }

    // === 地址管理 ===
    $scope.addressList = [];
    $scope.addressEntity = {};

    $scope.loadAddresses = function() {
        settingService.getAddressList().success(function(response) {
            $scope.addressList = response;
        });
    }

    $scope.saveAddress = function() {
        if (!$scope.addressEntity.contactPerson) {
            alert('请输入收货人姓名');
            return;
        }
        if (!$scope.addressEntity.mobile) {
            alert('请输入联系电话');
            return;
        }
        if (!$scope.addressEntity.address) {
            alert('请输入详细地址');
            return;
        }
        settingService.saveAddress($scope.addressEntity).success(function(response) {
            if (response.success) {
                $scope.addressEntity = {};
                $scope.loadAddresses();
            } else {
                alert(response.message || '保存失败');
            }
        });
    }

    $scope.deleteAddress = function(id) {
        if (confirm('确定要删除该地址吗？')) {
            settingService.deleteAddress(id).success(function(response) {
                if (response.success) {
                    $scope.loadAddresses();
                } else {
                    alert(response.message || '删除失败');
                }
            });
        }
    }

    $scope.setDefault = function(id) {
        settingService.setDefault(id).success(function(response) {
            if (response.success) {
                $scope.loadAddresses();
            }
        });
    }

    // === 个人信息 ===
    $scope.userInfo = {};

    $scope.loadUserInfo = function() {
        settingService.getUserInfo().success(function(response) {
            $scope.userInfo = response;
        });
    }

    $scope.saveUserInfo = function() {
        settingService.updateUserInfo($scope.userInfo).success(function(response) {
            if (response.success) {
                alert('保存成功');
            } else {
                alert(response.message || '保存失败');
            }
        });
    }

    // === 密码修改 ===
    $scope.passwordEntity = {};

    $scope.updatePassword = function() {
        if (!$scope.passwordEntity.oldPassword) {
            alert('请输入旧密码');
            return;
        }
        if (!$scope.passwordEntity.newPassword) {
            alert('请输入新密码');
            return;
        }
        if ($scope.passwordEntity.newPassword !== $scope.passwordEntity.confirmPassword) {
            alert('两次密码输入不一致');
            return;
        }
        settingService.updatePassword($scope.passwordEntity).success(function(response) {
            if (response.success) {
                alert('密码修改成功');
                $scope.passwordEntity = {};
            } else {
                alert(response.message || '密码修改失败');
            }
        }).error(function(){ alert('操作失败，请稍后重试'); });
    }

    // === 手机验证码 ===
    $scope.smsCooldown = 0;
    var countdownTimer = null;

    $scope.sendCode = function(phone) {
        if (!phone) {
            alert('请输入手机号');
            return;
        }
        settingService.sendCode(phone).success(function(response) {
            if (response.success) {
                alert('验证码已发送');
                $scope.smsCooldown = 60;
                countdownTimer = $interval(function() {
                    $scope.smsCooldown--;
                    if ($scope.smsCooldown <= 0) {
                        $interval.cancel(countdownTimer);
                    }
                }, 1000);
            } else {
                alert(response.message || '发送失败');
            }
        }).error(function(){ alert('操作失败，请稍后重试'); });
    }

    // === 消息提示 ===
    $scope.showMessage = function(type, msg, duration) {
        $scope.messageType = type;
        $scope.message = msg;
        $timeout(function() {
            $scope.message = '';
            $scope.messageType = '';
        }, duration || 3000);
    };
});
