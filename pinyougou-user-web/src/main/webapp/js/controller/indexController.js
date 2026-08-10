//首页控制器
app.controller('indexController',function($scope,$timeout,loginService,orderService){
	$scope.paginationConf={
		currentPage:1,
		itemsPerPage:10,
		totalItems:0
	};
	
	$scope.showName=function(){
			loginService.showName().success(
					function(response){
						$scope.loginName=response.loginName;
					}
			).error(function(){ alert('操作失败，请稍后重试'); });
	}
	
	$scope.searchEntity={};
	$scope.statusOptions=[
		{value:'',name:'全部状态'},
		{value:'1',name:'未付款'},
		{value:'2',name:'已付款'},
		{value:'3',name:'未发货'},
		{value:'4',name:'已发货'},
		{value:'5',name:'交易成功'},
		{value:'6',name:'交易关闭'}
	];
	
	$scope.findPage=function(page,rows){
		orderService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;
				$scope.paginationConf.totalItems=response.total;
			}			
		).error(function(){ alert('操作失败，请稍后重试'); });
	}
	
	$scope.reloadList=function(){
		$scope.search($scope.paginationConf.currentPage,$scope.paginationConf.itemsPerPage);
	}
	
	$scope.search=function(page,rows){
		orderService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;
				$scope.paginationConf.totalItems=response.total;
				$scope.paginationConf.currentPage=page;
				$scope.paginationConf.itemsPerPage=rows;
				$scope.buildPageNumbers();
			}			
		).error(function(){ alert('操作失败，请稍后重试'); });
	}
	
	$scope.buildPageNumbers=function(){
		$scope.pageNumbers=[];
		var totalPages=Math.ceil($scope.paginationConf.totalItems/$scope.paginationConf.itemsPerPage);
		var currentPage=$scope.paginationConf.currentPage;
		var showPages=5;
		
		var startPage=Math.max(1,currentPage-Math.floor(showPages/2));
		var endPage=Math.min(totalPages,startPage+showPages-1);
		
		if(endPage-startPage<showPages-1){
			startPage=Math.max(1,endPage-showPages+1);
		}
		
		for(var i=startPage;i<=endPage;i++){
			$scope.pageNumbers.push(i);
		}
		
		$scope.totalPages=totalPages;
	}
	
	$scope.prevPage=function(){
		if($scope.paginationConf.currentPage>1){
			$scope.search($scope.paginationConf.currentPage-1,$scope.paginationConf.itemsPerPage);
		}
	}
	
	$scope.nextPage=function(){
		var totalPages=Math.ceil($scope.paginationConf.totalItems/$scope.paginationConf.itemsPerPage);
		if($scope.paginationConf.currentPage<totalPages){
			$scope.search($scope.paginationConf.currentPage+1,$scope.paginationConf.itemsPerPage);
		}
	}
	
	$scope.goToPage=function(page){
		var totalPages=Math.ceil($scope.paginationConf.totalItems/$scope.paginationConf.itemsPerPage);
		if(page>=1&&page<=totalPages){
			$scope.search(page,$scope.paginationConf.itemsPerPage);
		}else{
			$scope.showMessage('error','页码超出范围',2000);
		}
	}
	
	$scope.jumpToPage=function(){
		var page=parseInt($scope.jumpPageNum);
		if(isNaN(page)||page<1){
			$scope.showMessage('error','请输入有效的页码',2000);
			return;
		}
		$scope.goToPage(page);
	}
	
	$scope.getStatusName=function(status){
		for(var i=0;i<$scope.statusOptions.length;i++){
			if($scope.statusOptions[i].value==status){
				return $scope.statusOptions[i].name;
			}
		}
		return '未知状态';
	}
	
	$scope.showMessage=function(type,msg,duration){
		$scope.messageType=type;
		$scope.message=msg;
		// 使用 $timeout 替代 setTimeout，确保 AngularJS digest cycle 正确执行
		$timeout(function(){
			$scope.message='';
			$scope.messageType='';
		},duration||3000);
	}
});