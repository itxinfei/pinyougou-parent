 //控制层
app.controller('userController' ,function($scope,$controller,$location,$interval ,userService){

	//注册用户
	$scope.reg=function(){

		//比较两次输入的密码是否一致
		if($scope.password!=$scope.entity.password){
			alert("两次输入密码不一致，请重新输入");
			$scope.entity.password="";
			$scope.password="";
			return ;
		}
		//新增
		userService.add($scope.entity,$scope.smscode).success(
			function(response){
				if(response.success){
					alert("注册成功，即将跳转到登录页面");
					// 注册成功后自动跳转到登录页
					window.location.href="login.html";
				} else {
					alert(response.message);
				}
			}
		);
	}

	// 倒计时秒数
	$scope.smsCooldown = 0;
	var countdownTimer = null;

	// 页面销毁时取消定时器，防止内存泄露
	$scope.$on('$destroy', function() {
		if (countdownTimer) {
			$interval.cancel(countdownTimer);
		}
	});

	//发送验证码
	$scope.sendCode=function(){
		if($scope.entity.phone==null || $scope.entity.phone==""){
			alert("请填写手机号码");
			return ;
		}

		// 如果正在倒计时，不允许重复发送
		if($scope.smsCooldown > 0){
			return;
		}

		userService.sendCode($scope.entity.phone).success(
			function(response){
				if(response.success){
					alert("验证码已发送");
					// 使用 $interval 替代 setInterval，确保 AngularJS digest cycle 正确执行
					$scope.smsCooldown = 60;
					countdownTimer = $interval(function(){
						$scope.smsCooldown--;
						if($scope.smsCooldown <= 0){
							$interval.cancel(countdownTimer);
							countdownTimer = null;
						}
					}, 1000);
				} else {
					alert(response.message);
				}
			}
		);
	}

});
