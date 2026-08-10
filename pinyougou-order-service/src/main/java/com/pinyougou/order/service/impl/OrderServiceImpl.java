package com.pinyougou.order.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.exception.InsufficientStockException;
import com.pinyougou.exception.ResourceNotFoundException;
import com.pinyougou.exception.ValidationException;
import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbOrderExample;
import com.pinyougou.pojo.TbOrderExample.Criteria;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbOrderItemExample;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.pojo.group.Cart;
import com.pinyougou.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;

import entity.PageResult;
import util.IdWorker;
import org.apache.log4j.Logger;

/**
 * 服务实现层
 * <p>
 * 事务策略：
 * - 类级别不添加@Transactional
 * - add()方法通过编程式事务管理
 * - 每个商家订单独立事务
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = Logger.getLogger(OrderServiceImpl.class);

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private TbPayLogMapper payLogMapper;

    @Autowired
    private TbItemMapper itemMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbOrder> findAll() {
        return orderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private TbOrderItemMapper orderItemMapper;

    // ✅ 编程式事务管理：每个商家订单独立事务
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 增加订单（从购物车创建订单）
     * <p>
     * 业务逻辑：
     * 1. 校验订单基本信息（收货人、联系方式等）
     * 2. 从Redis获取用户购物车列表
     * 3. 遍历每个商家的购物车，为每个商家创建一个订单
     * 4. ✅ 每个商家订单独立事务（提高系统可用性）
     * 5. 生成支付日志（仅针对在线支付类型）
     * 6. 清空购物车
     * <p>
     * ✅ 已优化：事务边界
     * - 旧方案：一个大事务包裹所有订单
     *   - 问题：一个订单失败全部回滚
     *   - 问题：事务过大，锁定时间长
     * - 新方案：每个商家订单独立事务
     *   - 优势：一个订单失败不影响其他订单
     *   - 优势：事务粒度小，锁定时间短
     *   - 优势：提高系统并发能力
     * <p>
     * ⚠️ 注意事项：
     * - 事务失败时记录日志，继续处理其他商家
     * - 最终返回成功和失败的订单列表
     * - TODO: 需要调整返回值为包含成功/失败信息的Map
     *
     * @param order 订单基本信息（收货人、支付方式等）
     */
    @Override
    public void add(TbOrder order) {

        // ========== 第一步：参数校验 ==========
        if(order==null){
            throw new ValidationException("订单信息不能为空");
        }
        if(order.getUserId()==null||order.getUserId().trim().isEmpty()){
            throw new ValidationException("用户ID不能为空");
        }
        if(order.getPaymentType()==null||order.getPaymentType().trim().isEmpty()){
            throw new ValidationException("支付方式不能为空");
        }
        if(order.getReceiver()==null||order.getReceiver().trim().isEmpty()){
            throw new ValidationException("收货人不能为空");
        }
        if(order.getReceiverMobile()==null||order.getReceiverMobile().trim().isEmpty()){
            throw new ValidationException("收货人电话不能为空");
        }
        if(order.getReceiverAreaName()==null||order.getReceiverAreaName().trim().isEmpty()){
            throw new ValidationException("收货地址不能为空");
        }

        // ========== 第二步：从Redis获取购物车 ==========
        // key 格式: cartList -> HashMap(userId -> List<Cart>)
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());

        if(cartList==null||cartList.isEmpty()){
            throw new ValidationException("购物车为空，无法创建订单");
        }

        // 初始化订单ID列表和总金额
        List<String> orderIdList = new ArrayList<>();
        // ✅ 使用BigDecimal替代double计算金额（避免精度损失）
        // BigDecimal特点：任意精度、适合货币计算
        // 使用String.valueOf()构造器避免double精度损失
        BigDecimal total_money = BigDecimal.ZERO;

        // ✅ 记录成功和失败的订单
        List<String> successOrderIds = new ArrayList<>();
        List<String> failedOrderIds = new ArrayList<>();

        // ========== 第三步：遍历购物车，为每个商家创建订单 ==========
        // 每个购物车(Cart)代表一个商家的商品集合，生成一个独立订单
        // ✅ 收集成功创建的商家sellerId，用于后续精准清空购物车
        List<String> successSellerIds = new ArrayList<>();
        for (Cart cart : cartList) {
            try {
                String sellerId = transactionTemplate.execute(status -> {
                    return createOrderByCart(order, cart, orderIdList);
                });
                if (sellerId != null) {
                    successSellerIds.add(sellerId);
                    // ✅ 修复：将成功创建的订单ID添加到 successOrderIds（用于生成支付日志）
                    if (!orderIdList.isEmpty()) {
                        successOrderIds.add(orderIdList.get(orderIdList.size() - 1));
                    }
                }
                // 累加总金额
                // 注意：total_money 在成功时累加（从 createOrderByCart 返回的 tbOrder 中获取）
                // 由于 createOrderByCart 现在返回 sellerId，需要另一种方式获取金额
                // 这里重新计算：从 orderIdList 中查询已创建的订单
            } catch (Exception e) {
                // ✅ 事务失败时记录日志，继续处理其他商家订单
                logger.error("创建订单失败: userId=" + order.getUserId() + ", sellerId=" + cart.getSellerId(), e);
                failedOrderIds.add(cart.getSellerId());
                // 继续处理下一个商家订单（不中断整个流程）
            }
        }

        // ========== 第四步：生成支付日志（仅在线支付） ==========
        // 支付方式 "1" 表示在线支付（微信/支付宝），"2" 表示货到付款
        // 从已创建的订单中计算总金额
        if (!successOrderIds.isEmpty() && "1".equals(order.getPaymentType())) {
            for (String orderIdStr : successOrderIds) {
                TbOrder createdOrder = orderMapper.selectByPrimaryKey(Long.valueOf(orderIdStr));
                if (createdOrder != null) {
                    total_money = total_money.add(createdOrder.getPayment());
                }
            }
            // 生成支付日志
            TbPayLog payLog = new TbPayLog();

            // 支付日志ID（交易流水号）
            payLog.setOutTradeNo(idWorker.nextId() + "");
            payLog.setCreateTime(new Date());
            payLog.setUserId(order.getUserId());

            // ⚠️ 拼接订单ID列表，使用字符串替换去除方括号
            // 格式: "123456,789012,345678"
            payLog.setOrderList(successOrderIds.toString().replace("[", "").replace("]", ""));

            // ✅ 金额转换：使用BigDecimal精确计算，避免double精度损失
            // 元转分：multiply(100) 乘以100
            // setScale(0, RoundingMode.HALF_UP) 四舍五入取整
            payLog.setTotalFee(total_money.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValue());

            payLog.setTradeState("0");  // 交易状态：0-未支付
            payLog.setPayType("1");     // 支付类型：1-微信支付
            payLogMapper.insert(payLog);

            // 将支付日志存入Redis，供支付回调时使用
            redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);
        }

        // ========== 第五步：清空购物车（仅清空成功创建的商家商品） ==========
        // ⚠️ 修复：只清空成功创建订单的商家购物车，失败商家的商品保留
        if (!successSellerIds.isEmpty()) {
            List<Cart> remainingCartList = new ArrayList<>();
            for (Cart cart : cartList) {
                if (!successSellerIds.contains(cart.getSellerId())) {
                    // 该商家订单创建失败，保留其购物车商品
                    remainingCartList.add(cart);
                }
            }
            // 更新Redis中的购物车（仅保留失败商家的商品）
            redisTemplate.boundHashOps("cartList").put(order.getUserId(), remainingCartList);
        }

        // ========== 第六步：记录订单创建结果 ==========
        if (!failedOrderIds.isEmpty()) {
            logger.warn("部分订单创建失败: userId=" + order.getUserId() +
                       ", successCount=" + successOrderIds.size() +
                       ", failedCount=" + failedOrderIds.size() +
                       ", failedSellerIds=" + failedOrderIds);
        }
    }

    /**
     * 为单个商家创建订单（在独立事务中执行）
     * <p>
     * 事务边界：
     * - 此方法在TransactionTemplate的回调中执行
     * - 所有数据库操作在同一个事务中
     * - 失败则整个事务回滚
     * <p>
     * 执行步骤：
     * 1. 生成订单ID和订单实体
     * 2. 批量查询商品信息
     * 3. 遍历订单项，扣减库存
     * 4. 保存订单项
     * 5. 保存订单主表
     * <p>
     * @param order 订单基本信息
     * @param cart 商家购物车
     * @param orderIdList 订单ID列表（用于收集）
     * @return 成功时返回 sellerId，失败时返回 null
     */
    private String createOrderByCart(TbOrder order, Cart cart, List<String> orderIdList) {
        TbOrder tbOrder = new TbOrder();
        // 生成订单ID
        Long orderId = idWorker.nextId();
        tbOrder.setOrderId(orderId);
        tbOrder.setUserId(order.getUserId());
        tbOrder.setPaymentType(order.getPaymentType());
        tbOrder.setReceiver(order.getReceiver());
        tbOrder.setReceiverMobile(order.getReceiverMobile());
        tbOrder.setReceiverAreaName(order.getReceiverAreaName());
        tbOrder.setSellerId(cart.getSellerId());
        tbOrder.setStatus("1"); // 未付款
        tbOrder.setCreateTime(new Date());

        // 计算总金额
        BigDecimal totalPayment = BigDecimal.ZERO;
        for (TbOrderItem item : cart.getOrderItemList()) {
            item.setId(idWorker.nextId());
            item.setOrderId(orderId);
            item.setSellerId(cart.getSellerId());
            // 金额 = 单价 × 数量
            BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getNum()));
            item.setTotalFee(itemTotal);
            totalPayment = totalPayment.add(itemTotal);
            // 批量插入订单项
            orderItemMapper.insert(item);
            // ✅ 修复：扣减商品库存（使用数据库层面的原子更新，防止超卖）
            // SQL: update tb_item set stock_count = stock_count - ? where id = ? and stock_count >= ?
            int affected = itemMapper.decreaseStockCount(item.getItemId(), item.getNum());
            if (affected <= 0) {
                // 库存不足，抛出异常触发事务回滚
                throw new InsufficientStockException("商品库存不足，商品ID：" + item.getItemId());
            }
        }
        tbOrder.setPayment(totalPayment);

        // 保存订单主表
        orderMapper.insert(tbOrder);
        orderIdList.add(orderId + "");
        return cart.getSellerId();
    }

    /**
     * 修改
     */
    @Override
    @Transactional
    public void update(TbOrder order) {
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbOrder findOne(Long id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    @Transactional
    public void delete(Long[] ids) {
        for (Long id : ids) {
            orderMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbOrderExample example = new TbOrderExample();
        Criteria criteria = example.createCriteria();

        if (order != null) {
            if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
                criteria.andPaymentTypeLike("%" + order.getPaymentType() + "%");
            }
            if (order.getPostFee() != null && order.getPostFee().length() > 0) {
                criteria.andPostFeeLike("%" + order.getPostFee() + "%");
            }
            if (order.getStatus() != null && order.getStatus().length() > 0) {
                criteria.andStatusLike("%" + order.getStatus() + "%");
            }
            if (order.getShippingName() != null && order.getShippingName().length() > 0) {
                criteria.andShippingNameLike("%" + order.getShippingName() + "%");
            }
            if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
                criteria.andShippingCodeLike("%" + order.getShippingCode() + "%");
            }
            if (order.getUserId() != null && order.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + order.getUserId() + "%");
            }
            if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
                criteria.andBuyerMessageLike("%" + order.getBuyerMessage() + "%");
            }
            if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
                criteria.andBuyerNickLike("%" + order.getBuyerNick() + "%");
            }
            if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
                criteria.andBuyerRateLike("%" + order.getBuyerRate() + "%");
            }
            if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
                criteria.andReceiverAreaNameLike("%" + order.getReceiverAreaName() + "%");
            }
            if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + order.getReceiverMobile() + "%");
            }
            if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
                criteria.andReceiverZipCodeLike("%" + order.getReceiverZipCode() + "%");
            }
            if (order.getReceiver() != null && order.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + order.getReceiver() + "%");
            }
            if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
                criteria.andInvoiceTypeLike("%" + order.getInvoiceType() + "%");
            }
            if (order.getSourceType() != null && order.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + order.getSourceType() + "%");
            }
            if (order.getSellerId() != null && order.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + order.getSellerId() + "%");
            }

        }
        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public TbPayLog searchPayLogFromRedis(String userId) {
        return (TbPayLog) redisTemplate.boundHashOps("payLog").get(userId);
    }

    /**
     * 支付成功后更新订单状态
     * <p>
     * 执行流程：
     * 1. 根据支付流水号查询支付日志
     * 2. 更新支付日志状态为已支付，记录交易流水号
     * 3. 解析订单列表（支付日志中保存了该支付对应的所有订单ID）
     * 4. 批量更新订单状态为已付款
     * 5. 清除Redis中的支付日志
     * <p>
     * 业务场景：
     * - 微信支付回调时调用此方法
     * - 支持一个支付包含多个订单（合并支付场景）
     *
     * @param out_trade_no 支付流水号（对应TbPayLog的outTradeNo）
     * @param transaction_id 微信支付流水号（用于退款等后续操作）
     */
    @Transactional
    @Override
    public void updateOrderStatus(String out_trade_no, String transaction_id) {
        // ========== 第一步：更新支付日志 ==========
        TbPayLog payLog = payLogMapper.selectByPrimaryKey(out_trade_no);

        if (payLog == null) {
            throw new ResourceNotFoundException("支付日志不存在，流水号：" + out_trade_no);
        }

        payLog.setPayTime(new Date());             // 支付时间
        payLog.setTradeState("1");                 // 交易状态：0-未支付 -> 1-已支付
        payLog.setTransactionId(transaction_id);   // 微信交易流水号（退款时需要使用）

        payLogMapper.updateByPrimaryKey(payLog);

        // ========== 第二步：批量更新订单状态 ==========
        // 从支付日志中提取订单ID列表（逗号分隔的字符串）
        String orderList = payLog.getOrderList();
        String[] orderIds = orderList.split(",");

        for (String orderId : orderIds) {
            TbOrder order = orderMapper.selectByPrimaryKey(Long.valueOf(orderId));

            if (order != null) {
                order.setStatus("2");              // 订单状态：2-已付款
                order.setPaymentTime(new Date());  // 付款时间
                orderMapper.updateByPrimaryKey(order);
            } else {
                logger.warn("订单不存在，可能已被删除: " + orderId);
            }
        }

        // ========== 第三步：清除支付日志缓存 ==========
        // 支付日志处理完成后，从Redis中删除
        redisTemplate.boundHashOps("payLog").delete(payLog.getUserId());
    }

    /**
     * 更新订单状态（用户操作：取消订单、确认收货等）
     */
    @Override
    public void updateStatus(Long orderId, String status) {
        TbOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单不存在，ID：" + orderId);
        }

        // 校验状态流转合法性
        String currentStatus = order.getStatus();
        if (!isValidStatusTransition(currentStatus, status)) {
            throw new ValidationException("订单状态不允许此操作");
        }

        order.setStatus(status);
        orderMapper.updateByPrimaryKey(order);

        // 如果取消订单，需要恢复库存
        if ("6".equals(status) && "1".equals(currentStatus)) {
            TbOrderItemExample itemExample = new TbOrderItemExample();
            itemExample.createCriteria().andOrderIdEqualTo(orderId);
            List<TbOrderItem> items = orderItemMapper.selectByExample(itemExample);
            for (TbOrderItem item : items) {
                TbItem tbItem = itemMapper.selectByPrimaryKey(item.getItemId());
                if (tbItem != null) {
                    tbItem.setNum(tbItem.getNum() + item.getNum());
                    tbItem.setStatus("1");
                    itemMapper.updateByPrimaryKey(tbItem);
                }
            }
        }
    }

    /**
     * 校验订单状态流转是否合法
     */
    private boolean isValidStatusTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        // 1(未付款) -> 6(已关闭)：取消订单
        if ("1".equals(currentStatus) && "6".equals(targetStatus)) {
            return true;
        }
        // 4(已发货) -> 5(已收货)：确认收货
        if ("4".equals(currentStatus) && "5".equals(targetStatus)) {
            return true;
        }
        return false;
    }
}
