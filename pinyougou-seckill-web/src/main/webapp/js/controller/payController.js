app.controller('payController' ,function($scope ,$location,payService){


	$scope.createNative=function(){
		payService.createNative().success(
			function(response){

				if(!response) {
					alert('支付信息加载失败');
					return;
				}

				//显示订单号和金额
				$scope.money= response.total_fee ? (response.total_fee/100).toFixed(2) : '0.00';
				$scope.out_trade_no=response.out_trade_no;

				if(!response.code_url) {
					alert('支付二维码生成失败');
					return;
				}

				//生成二维码
				 var qr=new QRious({
					    element:document.getElementById('qrious'),
						size:250,
						value:response.code_url,
						level:'H'
			     });

				 queryPayStatus();//调用查询

			}
		).error(function(){ alert('操作失败，请稍后重试'); });
	}

	//调用查询
	function queryPayStatus(){
		if(!$scope.out_trade_no) {
			return;
		}
		payService.queryPayStatus($scope.out_trade_no).success(
			function(response){
				if(response.success){
					location.href="paysuccess.html#?money="+$scope.money;
				}else{
					// 检查是否是二维码超时（使用code而非message判断，更可靠）
					if(response.code === '二维码超时' || (response.message && response.message.indexOf('超时') >= 0)){
						alert("二维码超时，请刷新页面重新生成");
					}else{
						location.href="payfail.html";
					}
				}
			}
		).error(function(){ alert('操作失败，请稍后重试'); });
	}

	//获取金额
	$scope.getMoney=function(){
		return $location.search()['money'];
	}

});