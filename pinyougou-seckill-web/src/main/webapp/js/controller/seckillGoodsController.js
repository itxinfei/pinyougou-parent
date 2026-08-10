//控制层
app.controller('seckillGoodsController' ,function($scope,$location,seckillGoodsService,$interval){

	// 倒计时定时器（用于页面销毁时取消）
	var countdownTimer = null;

	// 页面销毁时取消定时器，防止内存泄露
	$scope.$on('$destroy', function() {
		if (countdownTimer) {
			$interval.cancel(countdownTimer);
		}
	});

	//读取列表数据绑定到表单中
	$scope.findList=function(){
		seckillGoodsService.findList().success(
			function(response){
				$scope.list=response;
			}
		);
	}

	//查询商品
	$scope.findOne=function(){
		//接收参数ID
		var id= $location.search()['id'];
		if(!id) {
			$scope.errorMessage = '商品ID不能为空';
			return;
		}
		seckillGoodsService.findOne(id).success(
			function(response){
				$scope.entity=response;

				if(!$scope.entity || !$scope.entity.endTime) {
					$scope.errorMessage = '商品信息加载失败';
					return;
				}

				//倒计时开始
				//获取从结束时间到当前日期的秒数
				var allsecond=  Math.floor( (new Date($scope.entity.endTime).getTime()- new Date().getTime())/1000 );

				if(allsecond <= 0) {
					$scope.timeString= "已结束";
					return;
				}

				countdownTimer= $interval(function(){
					allsecond=allsecond-1;
					$scope.timeString= convertTimeString(allsecond);

					if(allsecond<=0){
						$interval.cancel(countdownTimer);
						countdownTimer = null;
					}

				},1000 );

			}
		);
	}


	//转换秒为   天小时分钟秒格式  XXX天 10:22:33
	function convertTimeString(allsecond){
		var days= Math.floor( allsecond/(60*60*24));//天数
		var hours= Math.floor( (allsecond-days*60*60*24)/(60*60) );//小时数
		var minutes= Math.floor(  (allsecond -days*60*60*24 - hours*60*60)/60    );//分钟数
		var seconds= allsecond -days*60*60*24 - hours*60*60 -minutes*60; //秒数
		var timeString="";
		if(days>0){
			timeString=days+"天 ";
		}
		return timeString+hours+":"+minutes+":"+seconds;

	}

	//提交订单
	$scope.submitOrder=function(){
		if(!$scope.entity || !$scope.entity.id) {
			alert('商品信息未加载，请刷新页面重试');
			return;
		}
		seckillGoodsService.submitOrder( $scope.entity.id ).success(
			function(response){
				if(response.success){//如果下单成功
					alert("抢购成功，请在5分钟之内完成支付");
					location.href="pay.html";//跳转到支付页面
				}else{
					alert(response.message);
				}
			}
		);

	}

});