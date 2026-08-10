/**
 * 购物车数据模型
 * ✅ 已修复：替换模拟接口为真实后端API调用
 */
var cartModel = {

    // 加入购物车商品
    add : function (data, success, error) {
        czHttp.post('cart/addGoodsToCartList.do', data, function (responseData) {
            if(responseData.isok){
                success(responseData);
            } else {
                if (error) error(responseData);
            }
        });
    },

    // 删除购物车商品
    remove : function (data, success, error) {
        czHttp.post('cart/deleteItemFromCart.do', data, function (responseData) {
            if(responseData.isok){
                success(responseData);
            } else {
                if (error) error(responseData);
            }
        });
    },

    // 修改商品数量
    changeNumber : function (data, success, error) {
        czHttp.post('cart/addGoodsToCartList.do', data, function (responseData) {
            if(responseData.isok){
                success(responseData);
            } else {
                if (error) error(responseData);
            }
        });
    },

    // 购物车统计
    subtotal : function (success) {
        czHttp.get('cart/findCartList.do', function (responseData) {
            if(responseData.isok){
                success(responseData);
            }
        });
    },

    // 购物车列表
    list : function (success) {
        czHttp.get('cart/findCartList.do', function(responseData){
            success(responseData);
        });
    }
};