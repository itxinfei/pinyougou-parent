package com.pinyougou.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

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
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.pojo.group.Cart;
import com.pinyougou.order.service.OrderService;

import entity.PageResult;
import util.IdWorker;

/**
 * 服务实现层
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

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

    /**
     * 增加订单（从购物车创建订单）
     * <p>
     * 业务逻辑：
     * 1. 校验订单基本信息（收货人、联系方式等）
     * 2. 从Redis获取用户购物车列表
     * 3. 遍历每个商家的购物车，为每个商家创建一个订单
     * 4. 批量扣减商品库存（使用乐观锁防止超卖）
     * 5. 生成支付日志（仅针对在线支付类型）
     * 6. 清空购物车
     * <p>
     * ⚠️ 注意事项：
     * - 该方法在同一个事务中处理多个商家的订单，如果中途失败会全部回滚
     * - 库存扣减使用 SQL 层面的乐观锁（WHERE stock_count >= num），避免超卖
     * - 订单金额计算使用 double，存在精度损失风险，建议使用 BigDecimal
     * - TODO: 考虑拆分为每个商家独立事务，提高系统可用性
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

        // ========== 第三步：遍历购物车，为每个商家创建订单 ==========
        // 每个购物车(Cart)代表一个商家的商品集合，生成一个独立订单
        for (Cart cart : cartList) {
            TbOrder tbOrder = new TbOrder();

            // 3.1 生成订单ID（使用雪花算法保证全局唯一）
            long orderId = idWorker.nextId();
            tbOrder.setOrderId(orderId);

            // 3.2 设置订单基本信息
            tbOrder.setPaymentType(order.getPaymentType());  // 支付方式：1-在线支付，2-货到付款
            tbOrder.setStatus("1");                           // 订单状态：1-未付款，2-已付款，3-已发货，4-已收货，5-已关闭
            tbOrder.setCreateTime(new Date());
            tbOrder.setUpdateTime(new Date());
            tbOrder.setUserId(order.getUserId());
            tbOrder.setReceiverAreaName(order.getReceiverAreaName());
            tbOrder.setReceiverMobile(order.getReceiverMobile());
            tbOrder.setReceiver(order.getReceiver());
            tbOrder.setSourceType(order.getSourceType());     // 订单来源：1-PC端，2-移动端
            tbOrder.setSellerId(order.getSellerId());         // 商家ID（当前购物车所属商家）

            // 3.3 计算该商家的订单金额
            // ✅ 使用BigDecimal.ZERO初始化（替代double的0）
            BigDecimal money = BigDecimal.ZERO;
            List<Long> itemIds = new ArrayList<>();

            // 收集商品ID用于批量查询商品信息（减少数据库查询次数）
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                itemIds.add(orderItem.getItemId());
            }

            // 批量查询商品信息，构建商品ID到商品实体的映射
            Map<Long, TbItem> itemMap = new HashMap<>();
            if (!itemIds.isEmpty()) {
                List<TbItem> items = itemMapper.selectByIds(itemIds);
                for (TbItem item : items) {
                    itemMap.put(item.getId(), item);
                }
            }

            // 3.4 遍历订单项，验证商品、扣减库存、保存订单项
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                TbItem item = itemMap.get(orderItem.getItemId());

                // 商品不存在性检查
                if(item==null){
                    throw new ResourceNotFoundException("商品不存在，商品ID："+orderItem.getItemId());
                }

                // 扣减库存（使用乐观锁：UPDATE tb_item SET stock_count = stock_count - num WHERE id = ? AND stock_count >= ?）
                // 返回影响行数：0表示库存不足，1表示扣减成功
                // ⚠️ 并发安全：SQL层面的乐观锁防止超卖，但无法防止负数库存
                int result = itemMapper.decreaseStockCount(orderItem.getItemId(), orderItem.getNum());
                if(result==0){
                    throw new InsufficientStockException("库存不足，商品："+item.getTitle());
                }

                // 设置订单项主键和关联关系
                orderItem.setId(idWorker.nextId());       // 订单项ID
                orderItem.setOrderId(orderId);             // 关联订单ID
                orderItem.setSellerId(cart.getSellerId()); // 所属商家ID
                orderItemMapper.insert(orderItem);         // 保存订单项

                // ✅ 使用BigDecimal累加金额（避免double精度损失）
                // add() 方法精确相加，setScale() 确保小数位一致性
                money = money.add(orderItem.getTotalFee());
            }

            // 3.5 设置订单总金额（直接使用BigDecimal，避免转换损失）
            tbOrder.setPayment(money);

            // 3.6 保存订单主表
            orderMapper.insert(tbOrder);

            // 收集订单ID用于生成支付日志
            orderIdList.add(orderId + "");

            // ✅ 累加总金额（BigDecimal精确计算）
            total_money = total_money.add(money);
        }

        // ========== 第四步：生成支付日志（仅在线支付） ==========
        // 支付方式 "1" 表示在线支付（微信/支付宝），"2" 表示货到付款
        if ("1".equals(order.getPaymentType())) {
            TbPayLog payLog = new TbPayLog();

            // 支付日志ID（交易流水号）
            payLog.setOutTradeNo(idWorker.nextId() + "");
            payLog.setCreateTime(new Date());
            payLog.setUserId(order.getUserId());

            // ⚠️ 拼接订单ID列表，使用字符串替换去除方括号
            // 格式: "123456,789012,345678"
            payLog.setOrderList(orderIdList.toString().replace("[", "").replace("]", ""));

            // ✅ 金额转换：使用BigDecimal精确计算，避免double精度损失
            // 元转分：multiply(100) 乘以100
            // setScale(0, RoundingMode.HALF_UP) 四舍五入取整
            payLog.setTotalFee(total_money.multiply(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_HALF_UP).longValue());

            payLog.setTradeState("0");  // 交易状态：0-未支付
            payLog.setPayType("1");     // 支付类型：1-微信支付
            payLogMapper.insert(payLog);

            // 将支付日志存入Redis，供支付回调时使用
            redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);
        }

        // ========== 第五步：清空购物车 ==========
        // ⚠️ 注意：如果订单创建失败会回滚，但如果成功则立即清空购物车
        // 风险：用户可能想保留购物车商品用于下次购买
        // TODO: 考虑实现"合并购物车"功能，未生成订单的商品保留
        redisTemplate.boundHashOps("cartList").delete(order.getUserId());
    }


    /**
     * 修改
     */
    @Override
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
}
