app.controller("indexController",function($scope,loginService){
	
	$scope.showName = function(){
		loginService.showName().success(function(response){
			$scope.loginName = response.username;
		}).error(function(){ alert('操作失败，请稍后重试'); });
	}
	
});