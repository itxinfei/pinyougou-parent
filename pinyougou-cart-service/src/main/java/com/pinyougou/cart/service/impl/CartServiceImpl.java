package com.pinyougou.cart.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.exception.InsufficientStockException;
import com.pinyougou.exception.ResourceNotFoundException;
import com.pinyougou.exception.ValidationException;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.group.Cart;

/**
 * 购物车服务实现类
 *
 * @author Administrator
 */
@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = Logger.getLogger(CartServiceImpl.class);

    // 分布式锁配置
    private static final String CART_LOCK_PREFIX = "lock:cart:";
    private static final long CART_LOCK_TIMEOUT = 10; // 锁过期时间（秒）
    private static final int CART_LOCK_WAIT_COUNT = 10; // 等待锁的重试次数

    // ✅ 购物车商品总数上限（防止内存溢出）
    private static final int MAX_CART_ITEM_COUNT = 200; // 最多200件商品

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加商品到购物车
     * <p>
     * 业务逻辑：
     * 1. 获取分布式锁（防止并发修改）
     * 2. 参数校验（商品ID、数量合法性）
     * 3. 查询商品信息（验证商品存在性和状态）
     * 4. 库存校验（防止超卖）
     * 5. 根据商家ID查找购物车（一个商家对应一个Cart）
     * 6. 如果商品已在购物车中，则增加数量；否则新增商品
     * 7. 更新商品总价（price × num）
     * <p>
     * ✅ 已优化：线程安全
     * - 使用Redis分布式锁（lock:cart:{userId}）
     * - 锁粒度：按用户维度加锁（同一用户并发操作排队）
     * - 锁过期时间：10秒（防止死锁）
     * - 重试次数：最多等待5秒
     * <p>
     * 锁粒度说明：
     * - 用户维度：不同用户互不影响（推荐）
     * - 商品维度：同一商品所有用户排队（性能差）
     * - 全局维度：所有操作排队（性能最差）
     * <p>
     * 购物车数据结构：
     * List<Cart>
     *   ├─ Cart 1 (商家A)
     *   │   ├─ sellerId: "1001"
     *   │   ├─ sellerName: "联想旗舰店"
     *   │   └─ orderItemList: [OrderItem, OrderItem, ...]
     *   ├─ Cart 2 (商家B)
     *   │   ├─ sellerId: "1002"
     *   │   ├─ sellerName: "戴尔旗舰店"
     *   │   └─ orderItemList: [OrderItem, ...]
     * <p>
     * 数量限制：
     * - 单次添加数量：1-999件
     * - 累计数量限制：同一商品最多999件（防止超卖）
     * - 总购物车商品数：未限制（建议增加限制）
     * <p>
     * 异常处理：
     * - ValidationException: 参数错误、获取锁超时
     * - ResourceNotFoundException: 商品不存在
     * - InsufficientStockException: 库存不足
     *
     * @param userId 用户ID（用于分布式锁）
     * @param cartList 购物车列表（已有商品）
     * @param itemId 商品SKU ID
     * @param num 购买数量
     * @return 更新后的购物车列表
     */
    @Override
    public List<Cart> addGoodsToCartList(String userId, List<Cart> cartList, Long itemId, Integer num) {
        // ✅ 分布式锁：按用户ID加锁（优化锁粒度）
        // 锁键格式: lock:cart:{userId}
        // 优势：不同用户互不影响，只有同一用户的并发操作才需要排队
        String lockKey = CART_LOCK_PREFIX + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", CART_LOCK_TIMEOUT, TimeUnit.SECONDS);

        // 尝试获取锁
        int waitCount = 0;
        while (locked == null || !locked) {
            if (waitCount++ >= CART_LOCK_WAIT_COUNT) {
                throw new ValidationException("系统繁忙，请稍后重试");
            }
            try {
                Thread.sleep(500); // 500ms后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ValidationException("操作被中断");
            }
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", CART_LOCK_TIMEOUT, TimeUnit.SECONDS);
        }

        try {
            // ========== 参数校验 ==========
            if(itemId==null||itemId<=0){
                throw new ValidationException("商品ID不能为空且必须大于0");
            }
            if(num==null||num<=0){
                throw new ValidationException("商品数量不能为空且必须大于0");
            }
            if(num>999){
                throw new ValidationException("单次购买数量不能超过999件");
            }

            // ========== 购物车商品总数检查 ==========
            // 计算当前购物车商品总数
            int totalItemCount = 0;
            for (Cart cart : cartList) {
                totalItemCount += cart.getOrderItemList().size();
            }

            // 检查是否已添加过该商品
            boolean itemExists = false;
            for (Cart cart : cartList) {
                TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(), itemId);
                if (orderItem != null) {
                    itemExists = true;
                    break;
                }
            }

            // 如果商品不存在，且总数已达上限，则拒绝添加
            if (!itemExists && totalItemCount >= MAX_CART_ITEM_COUNT) {
                throw new ValidationException("购物车商品总数不能超过" + MAX_CART_ITEM_COUNT + "件，请先清理部分商品");
            }

            // ========== 商品信息校验 ==========
            TbItem item = itemMapper.selectByPrimaryKey(itemId);
            if(item==null){
                throw new ResourceNotFoundException("商品不存在");
            }
            if(!item.getStatus().equals("1")){
                throw new ValidationException("商品状态不合法");
            }
            if(item.getStockCount()==null||item.getStockCount()<=0){
                throw new InsufficientStockException("商品库存不足");
            }
            if(num>item.getStockCount()){
                throw new InsufficientStockException("购买数量超过库存，当前库存："+item.getStockCount());
            }

            String sellerId = item.getSellerId();

            // ========== 查找购物车 ==========
            Cart cart = searchCartBySellerId(cartList,sellerId);

            if(cart==null){
                // ========== 购物车不存在，创建新购物车 ==========
                cart=new Cart();
                cart.setSellerId(sellerId);
                cart.setSellerName(item.getSeller());
                List<TbOrderItem> orderItemList=new ArrayList();
                TbOrderItem orderItem = createOrderItem(item,num);
                orderItemList.add(orderItem);
                cart.setOrderItemList(orderItemList);

                cartList.add(cart);

            }else{
                // ========== 购物车已存在，查找商品 ==========
                TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(),itemId);
                if(orderItem==null){
                    // ========== 商品不存在，新增商品 ==========
                    orderItem=createOrderItem(item,num);
                    cart.getOrderItemList().add(orderItem);

                }else{
                    // ========== 商品已存在，增加数量 ==========
                    int newNum=orderItem.getNum()+num;
                    if(newNum>999){
                        throw new ValidationException("单次购买数量不能超过999件");
                    }
                    if(newNum>item.getStockCount()){
                        throw new InsufficientStockException("购买数量超过库存，当前库存："+item.getStockCount());
                    }
                    orderItem.setNum(newNum);
                    orderItem.setTotalFee(orderItem.getPrice().multiply(new BigDecimal(orderItem.getNum())));
                    if(orderItem.getNum()<=0){
                        cart.getOrderItemList().remove(orderItem);
                    }
                    if(cart.getOrderItemList().size()==0){
                        cartList.remove(cart);
                    }
                }

            }

            return cartList;

        } finally {
            // ========== 释放锁 ==========
            // 使用finally确保锁一定会被释放（防止死锁）
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 根据商家ID在购物车列表中查询购物车对象
     * <p>
     * 查找逻辑：遍历购物车列表，匹配sellerId
     * <p>
     * 性能优化建议：
     * - 当前实现：O(n) 线性查找
     * - 优化方案：使用 HashMap<String, Cart> 存储，O(1) 查找
     *
     * @param cartList 购物车列表
     * @param sellerId 商家ID
     * @return 购物车对象，不存在返回null
     */
    Cart searchCartBySellerId(List<Cart> cartList,String sellerId){
        for(Cart cart:cartList){
            if(cart.getSellerId().equals(sellerId)){
                return cart;
            }
        }
        return null;
    }

    /**
     * 根据SKU ID在购物车明细列表中查询购物车明细对象
     * <p>
     * 查找逻辑：遍历订单项列表，匹配itemId
     * <p>
     * 注意：使用longValue()比较，避免Integer/Long类型不匹配
     *
     * @param orderItemList 订单项列表
     * @param itemId 商品SKU ID
     * @return 订单项对象，不存在返回null
     */
    public TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
        for(TbOrderItem orderItem:orderItemList){
            if(orderItem.getItemId().longValue()==itemId.longValue()){
                return orderItem;
            }
        }
        return null;
    }

    /**
     * 创建购物车明细对象
     * <p>
     * 字段来源：
     * - goodsId: 商品SPU ID（用于批量删除索引）
     * - itemId: 商品SKU ID（唯一标识）
     * - price: 商品单价（从TbItem.price读取）
     * - totalFee: 商品总价 = price × num
     * <p>
     * 注意：totalFee使用BigDecimal计算，避免精度损失
     *
     * @param item 商品实体
     * @param num 购买数量
     * @return 订单项对象
     */
    TbOrderItem createOrderItem(TbItem item,Integer num){
        //创建新的购物车明细对象
        TbOrderItem orderItem=new TbOrderItem();
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setItemId(item.getId());
        orderItem.setNum(num);
        orderItem.setPicPath(item.getImage());
        orderItem.setPrice(item.getPrice());
        orderItem.setSellerId(item.getSellerId());
        orderItem.setTitle(item.getTitle());
        // 使用BigDecimal计算总价，避免double精度损失
        orderItem.setTotalFee(item.getPrice().multiply(new BigDecimal(num)));
        return orderItem;
    }

    @Autowired
    private RedisTemplate<String, Object> cartRedisTemplate;

    /**
     * 从Redis查询购物车列表
     * <p>
     * Redis结构：
     * - Key: cartList
     * - Type: Hash
     * - Field: userId
     * - Value: List<Cart>
     * <p>
     * 如果购物车不存在，返回空列表（避免空指针异常）
     *
     * @param username 用户名（作为Redis Hash的field）
     * @return 购物车列表
     */
    @Override
    public List<Cart> findCartListFromRedis(String username) {
        logger.info("从redis中提取购物车" + username);
        List<Cart> cartList = (List<Cart>) cartRedisTemplate.boundHashOps("cartList").get(username);
        if(cartList==null){
            cartList=new ArrayList();
        }
        return cartList;
    }

    /**
     * 保存购物车列表到Redis
     * <p>
     * Redis结构：
     * - Key: cartList
     * - Field: userId
     * - Value: List<Cart>（JSON序列化）
     * <p>
     * 注意：未设置过期时间（永久有效，用户主动清空或过期策略）
     *
     * @param username 用户名
     * @param cartList 购物车列表
     */
    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        logger.info("向redis中存入购物车" + username);
        cartRedisTemplate.boundHashOps("cartList").put(username, cartList);
    }

    /**
     * 合并购物车（用户登录时调用）
     * <p>
     * 业务场景：
     * - 用户未登录时添加了商品到本地购物车（购物车1）
     * - 用户登录时，登录前购物车已有商品（购物车2）
     * - 需要将两个购物车合并到登录账号的购物车中
     * <p>
     * 合并策略：
     * 1. 遍历购物车2的每个购物车和商品
     * 2. 直接合并商品到购物车1（使用HashMap索引）
     * 3. 如果商品已存在，则增加数量（最多999件）
     * 4. 如果商品不存在，则新增商品到对应商家的购物车
     * <p>
     * 合并规则：
     * - 按商家分组：不同商家的商品分别存放
     * - 相同商品合并：同一SKU的数量累加
     * - 数量限制：合并后总数不超过999件
     * - 库存校验：合并时不再验证库存（提升性能）
     * <p>
     * ✅ 已优化：
     * - 使用HashMap索引 O(n+m)
     * - 不再调用addGoodsToCartList（避免重复查询数据库）
     * - 直接操作内存对象，性能提升显著
     * <p>
     * TODO: 继续优化
     * - 添加商品状态验证（过滤已下架商品）
     * - 添加库存变化提醒（库存不足时提示用户）
     * - 合并后排序：按商家、按添加时间
     *
     * @param userId 用户ID（用于日志记录）
     * @param cartList1 登录用户的购物车（主购物车）
     * @param cartList2 未登录时的本地购物车（待合并）
     * @return 合并后的购物车列表
     */
    @Override
    public List<Cart> mergeCartList(String userId, List<Cart> cartList1, List<Cart> cartList2) {
        logger.info("合并购物车: userId=" + userId + ", cartList1.size=" + cartList1.size() + ", cartList2.size=" + cartList2.size());

        // ========== 第一步：构建HashMap索引（优化查找性能） ==========
        // 将cartList1转换为HashMap，key为sellerId，value为Cart对象
        // 时间复杂度：O(n)，只遍历一次cartList1
        Map<String, Cart> cartMap = new HashMap<>();
        for (Cart cart : cartList1) {
            cartMap.put(cart.getSellerId(), cart);
        }

        // ========== 第二步：遍历待合并的购物车 ==========
        for (Cart cart2 : cartList2) {
            String sellerId = cart2.getSellerId();
            Cart cart1 = cartMap.get(sellerId);

            if (cart1 == null) {
                // ========== 商家不存在，直接添加整个购物车 ==========
                cartList1.add(cart2);
                cartMap.put(sellerId, cart2);
            } else {
                // ========== 商家已存在，合并商品 ==========
                // 遍历待合并购物车中的商品
                for (TbOrderItem orderItem2 : cart2.getOrderItemList()) {
                    // 查找该商品是否已在购物车中
                    TbOrderItem existingItem = searchOrderItemByItemId(cart1.getOrderItemList(), orderItem2.getItemId());

                    if (existingItem == null) {
                        // ========== 商品不存在，直接添加 ==========
                        cart1.getOrderItemList().add(orderItem2);
                    } else {
                        // ========== 商品已存在，合并数量 ==========
                        int newNum = existingItem.getNum() + orderItem2.getNum();
                        if (newNum > 999) {
                            logger.warn("合并后数量超过上限: userId=" + userId + ", itemId=" + orderItem2.getItemId() + ", num=" + newNum);
                            newNum = 999; // 截断到上限
                        }
                        existingItem.setNum(newNum);
                        existingItem.setTotalFee(existingItem.getPrice().multiply(new BigDecimal(newNum)));
                    }
                }
            }
        }

        logger.info("购物车合并完成: userId=" + userId + ", 合并后大小=" + cartList1.size());
        return cartList1;
    }
                        }
                        existingItem.setNum(newNum);
                        existingItem.setTotalFee(existingItem.getPrice().multiply(new BigDecimal(newNum)));
                    }
                }
            }
        }

        return cartList1;
    }

}
