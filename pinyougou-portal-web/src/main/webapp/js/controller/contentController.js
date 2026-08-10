app.controller('contentController',function($scope,contentService){

	$scope.contentList=[];//广告列表

	$scope.findByCategoryId=function(categoryId){
		contentService.findByCategoryId(categoryId).success(
			function(response){
				$scope.contentList[categoryId]=response;
			}
		);
	}

	// 搜索功能
	$scope.keywords='';
	$scope.search=function(){
		if($scope.keywords && $scope.keywords.trim()){
			// 跳转到搜索页面
			location.href='../search-web/search.html#?keywords='+encodeURIComponent($scope.keywords.trim());
		}
	}

});