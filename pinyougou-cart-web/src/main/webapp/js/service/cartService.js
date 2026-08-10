//购物车服务层
app.service('cartService',function($http){
	//购物车列表
	this.findCartList=function(){
		return $http.get('cart/findCartList.do');
	}
	
	//添加商品到购物车
	this.addGoodsToCartList=function(itemId,num){
		return $http.get('cart/addGoodsToCartList.do?itemId='+itemId+'&num='+num);
	}
	
	//求合计数
	this.sum=function(cartList){
		var totalValue={totalNum:0,totalMoney:0 };

		// 防御性检查：cartList 为空时返回默认值
		if(!cartList || !Array.isArray(cartList)) {
			return totalValue;
		}

		for(var i=0;i<cartList.length ;i++){
			var cart=cartList[i];//购物车对象
			if(!cart || !cart.orderItemList) continue;

			for(var j=0;j<cart.orderItemList.length;j++){
				var orderItem=  cart.orderItemList[j];//购物车明细
				if(!orderItem) continue;

				totalValue.totalNum+=(orderItem.num || 0);//累加数量
				totalValue.totalMoney+=(orderItem.totalFee || 0);//累加金额
			}
		}
		return totalValue;

	}
	
	//获取当前登录账号的收货地址
	this.findAddressList=function(){
		return $http.get('address/findListByLoginUser.do');
	}
	
	//提交订单
	this.submitOrder=function(order){
		return $http.post('order/add.do',order);
	}

	//保存新地址
	this.saveAddress=function(addressEntity){
		return $http.post('address/add.do',addressEntity);
	}
	
	
});