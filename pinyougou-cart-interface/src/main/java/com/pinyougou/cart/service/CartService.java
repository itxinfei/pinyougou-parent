package com.pinyougou.cart.service;

import com.pinyougou.pojo.group.Cart;

import java.util.List;


/**
 * 购物车服务接口
 * <p>
 * 核心功能：
 * - 添加商品到购物车
 * - 购物车合并（登录时合并本地购物车）
 * - Redis存储和查询
 * <p>
 * 线程安全：
 * - 使用Redis分布式锁（lock:cart:{userId}）
 * - 锁粒度：按用户维度加锁
 *
 * @author Administrator
 */
public interface CartService {

    /**
     * 添加商品到购物车
     * <p>
     * 业务逻辑：
     * 1. 获取分布式锁（lock:cart:{userId}）
     * 2. 参数校验（商品ID、数量合法性）
     * 3. 查询商品信息
     * 4. 库存校验
     * 5. 根据商家ID查找购物车
     * 6. 商品已存在则增加数量，否则新增
     * <p>
     * 锁粒度：按用户ID加锁（避免影响其他用户）
     *
     * @param userId 用户ID（用于分布式锁）
     * @param cartList 购物车列表
     * @param itemId 商品SKU ID
     * @param num 购买数量
     * @return 更新后的购物车列表
     */
    public List<Cart> addGoodsToCartList(String userId, List<Cart> cartList, Long itemId, Integer num);

    /**
     * 从redis中提取购物车列表
     *
     * @param username 用户名
     * @return 购物车列表
     */
    public List<Cart> findCartListFromRedis(String username);

    /**
     * 将购物车列表存入redis
     *
     * @param username 用户名
     * @param cartList 购物车列表
     */
    public void saveCartListToRedis(String username, List<Cart> cartList);

    /**
     * 合并购物车（用户登录时调用）
     * <p>
     * 合并策略：
     * 1. 使用HashMap索引优化性能（O(n+m)）
     * 2. 按商家分组
     * 3. 相同商品数量累加（最多999件）
     * <p>
     * 性能优化：
     * - 原方案：嵌套循环 O(n×m)
     * - 新方案：HashMap索引 O(n+m)
     *
     * @param userId 用户ID（用于日志记录）
     * @param cartList1 登录用户的购物车（主购物车）
     * @param cartList2 未登录时的本地购物车（待合并）
     * @return 合并后的购物车列表
     */
    public List<Cart> mergeCartList(String userId, List<Cart> cartList1, List<Cart> cartList2);

}
