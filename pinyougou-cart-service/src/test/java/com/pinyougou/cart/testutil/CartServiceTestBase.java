package com.pinyougou.cart.testutil;

import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.group.Cart;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CartServiceImpl测试工具类
 * <p>
 * 提供购物车服务测试的公共方法：
 * 1. 创建测试用的商品数据
 * 2. 创建测试用的购物车数据
 * 3. Mock Redis和Mapper的常用操作
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class CartServiceTestBase extends ServiceTestBase {

    /**
     * 创建测试商品（默认数据）
     *
     * @return TbItem对象
     */
    protected TbItem createTestItem() {
        TbItem item = new TbItem();
        item.setId(1001L);
        item.setGoodsId(2001L);
        item.setTitle("测试商品");
        item.setPrice(new BigDecimal("99.99"));
        item.setStockCount(100);
        item.setStatus("1");
        item.setSellerId("seller_001");
        item.setSeller("测试商家");
        item.setImage("test.jpg");
        return item;
    }

    /**
     * 创建测试商品（自定义数据）
     *
     * @param id        商品ID
     * @param title     商品标题
     * @param price     商品价格
     * @param stockCount 库存数量
     * @param sellerId  商家ID
     * @param status    商品状态
     * @return TbItem对象
     */
    protected TbItem createTestItem(Long id, String title, BigDecimal price,
                                     Integer stockCount, String sellerId, String status) {
        TbItem item = new TbItem();
        item.setId(id);
        item.setGoodsId(id);
        item.setTitle(title);
        item.setPrice(price);
        item.setStockCount(stockCount);
        item.setStatus(status);
        item.setSellerId(sellerId);
        item.setSeller("测试商家" + sellerId);
        item.setImage("test.jpg");
        return item;
    }

    /**
     * 创建测试订单项
     *
     * @param item 商品对象
     * @param num  购买数量
     * @return TbOrderItem对象
     */
    protected TbOrderItem createOrderItem(TbItem item, Integer num) {
        TbOrderItem orderItem = new TbOrderItem();
        orderItem.setItemId(item.getId());
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setNum(num);
        orderItem.setPrice(item.getPrice());
        orderItem.setTotalFee(item.getPrice().multiply(new BigDecimal(num)));
        orderItem.setTitle(item.getTitle());
        orderItem.setPicPath(item.getImage());
        orderItem.setSellerId(item.getSellerId());
        return orderItem;
    }

    /**
     * 创建测试购物车
     *
     * @param sellerId 商家ID
     * @param sellerName 商家名称
     * @param orderItems 订单项列表
     * @return Cart对象
     */
    protected Cart createCart(String sellerId, String sellerName, List<TbOrderItem> orderItems) {
        Cart cart = new Cart();
        cart.setSellerId(sellerId);
        cart.setSellerName(sellerName);
        cart.setOrderItemList(orderItems);
        return cart;
    }

    /**
     * 创建测试购物车（单个商品）
     *
     * @param sellerId 商家ID
     * @param sellerName 商家名称
     * @param item 商品对象
     * @param num 购买数量
     * @return Cart对象
     */
    protected Cart createCart(String sellerId, String sellerName, TbItem item, Integer num) {
        List<TbOrderItem> orderItems = new ArrayList<>();
        orderItems.add(createOrderItem(item, num));
        return createCart(sellerId, sellerName, orderItems);
    }

    /**
     * Mock Redis分布式锁获取成功
     *
     * @param redisTemplate RedisTemplate
     */
    protected void mockLockAcquired(RedisTemplate<String, Object> redisTemplate) {
        when(redisTemplate.opsForValue()
            .setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
    }

    /**
     * Mock Redis分布式锁获取失败
     *
     * @param redisTemplate RedisTemplate
     */
    protected void mockLockFailed(RedisTemplate<String, Object> redisTemplate) {
        when(redisTemplate.opsForValue()
            .setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(false);
    }

    /**
     * Mock Redis删除操作
     *
     * @param redisTemplate RedisTemplate
     */
    protected void mockRedisDelete(RedisTemplate<String, Object> redisTemplate) {
        // doNothing()是默认行为，但显式声明更清晰
        org.mockito.Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());
    }
}
