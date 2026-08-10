//购物车控制层
app.controller('cartController',function($scope,cartService){
	$scope.errorMessage = '';
	$scope.successMessage = '';
	
	$scope.showMessage = function(type, msg, duration) {
		if(type === 'error') {
			$scope.errorMessage = msg;
			$scope.successMessage = '';
		} else {
			$scope.successMessage = msg;
			$scope.errorMessage = '';
		}
		setTimeout(function() {
			$scope.errorMessage = '';
			$scope.successMessage = '';
			$scope.$apply();
		}, duration || 3000);
	};
	
	$scope.clearMessages = function() {
		$scope.errorMessage = '';
		$scope.successMessage = '';
	};
	
	$scope.validateItemId = function(itemId) {
		if(!itemId || itemId <= 0) {
			$scope.showMessage('error', '商品ID无效');
			return false;
		}
		return true;
	};
	
	$scope.validateNum = function(num) {
		if(!num || num === '') {
			$scope.showMessage('error', '商品数量不能为空');
			return false;
		}
		var numValue = parseInt(num);
		if(isNaN(numValue) || numValue < 1) {
			$scope.showMessage('error', '商品数量必须大于0');
			return false;
		}
		if(numValue > 999) {
			$scope.showMessage('error', '单次购买数量不能超过999件');
			return false;
		}
		return true;
	};
	
	$scope.findCartList=function(){
		$scope.clearMessages();
		cartService.findCartList().success(
			function(response){
				$scope.cartList=response;
				$scope.totalValue= cartService.sum($scope.cartList);
			}
		).error(
			function(data, status, headers, config){
				$scope.showMessage('error', '获取购物车列表失败，请稍后重试');
			}
		);
	}
	
	$scope.addGoodsToCartList=function(itemId,num){
		$scope.clearMessages();
		if(!$scope.validateItemId(itemId)) {
			return;
		}
		if(!$scope.validateNum(num)) {
			return;
		}
		cartService.addGoodsToCartList(itemId,num).success(
			function(response){
				if(response.success){
					$scope.findCartList();
					$scope.showMessage('success', '购物车已更新');
				}else{
					$scope.showMessage('error', response.message || '操作失败');
				}				
			}
		).error(
			function(data, status, headers, config){
				$scope.showMessage('error', '网络请求失败，请检查网络连接');
			}
		);		
	}

	$scope.findAddressList=function(){
		$scope.clearMessages();
		cartService.findAddressList().success(
			function(response){
				$scope.addressList=response;
				for(var i=0;i<$scope.addressList.length;i++){
					if($scope.addressList[i].isDefault=='1'){
						$scope.address=$scope.addressList[i];
						break;
					}					
				}
			}
		).error(
			function(data, status, headers, config){
				$scope.showMessage('error', '获取地址列表失败，请稍后重试');
			}
		);		
	}
	
	$scope.selectAddress=function(address){
		$scope.clearMessages();
		$scope.address=address;		
	}
	
	$scope.isSeletedAddress=function(address){
		if(address==$scope.address){
			return true;
		}else{
			return false;
		}		
	}
	
	$scope.order={paymentType:'1'};
	
	$scope.selectPayType=function(type){
		$scope.clearMessages();
		$scope.order.paymentType=type;
	}
	
	$scope.validateOrder=function() {
		if(!$scope.address) {
			$scope.showMessage('error', '请选择收货地址');
			return false;
		}
		if(!$scope.order.paymentType) {
			$scope.showMessage('error', '请选择支付方式');
			return false;
		}
		if(!$scope.cartList || $scope.cartList.length === 0) {
			$scope.showMessage('error', '购物车为空，无法提交订单');
			return false;
		}
		return true;
	};
	
	$scope.submitOrder=function(){
		$scope.clearMessages();
		if(!$scope.validateOrder()) {
			return;
		}
		$scope.order.receiverAreaName=$scope.address.address;
		$scope.order.receiverMobile=$scope.address.mobile;
		$scope.order.receiver=$scope.address.contact;
		
		cartService.submitOrder($scope.order).success(
			function(response){
				if(response.success){
					if($scope.order.paymentType=='1'){
						location.href="pay.html";
					}else{
						location.href="paysuccess.html";
					}
				}else{
					$scope.showMessage('error', response.message || '提交订单失败');
				}
			}
		).error(
			function(data, status, headers, config){
				$scope.showMessage('error', '网络请求失败，请检查网络连接');
			}
		);		
	}
	
});