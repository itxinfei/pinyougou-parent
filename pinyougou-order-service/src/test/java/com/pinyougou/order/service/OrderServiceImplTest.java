package com.pinyougou.order.service.impl;

import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbPayLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import util.IdWorker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 订单服务测试类
 * <p>
 * 测试覆盖：
 * - 订单查询（findAll、findOne、findPage）
 * - 订单创建（add）
 * - 订单更新（update）
 * - 订单删除（delete）
 * - 支付状态更新（updateOrderStatus）
 * - 支付日志查询（searchPayLogFromRedis）
 *
 * @author Administrator
 */
@RunWith(MockitoJUnitRunner.class)
public class OrderServiceImplTest {

    @Mock
    private TbOrderMapper orderMapper;

    @Mock
    private TbPayLogMapper payLogMapper;

    @Mock
    private TbItemMapper itemMapper;

    @Mock
    private TbOrderItemMapper orderItemMapper;

    @Mock
    private IdWorker idWorker;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate redisTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private TbOrder testOrder;
    private TbPayLog testPayLog;

    /**
     * 测试前置准备
     */
    @Before
    public void setUp() {
        // 准备测试订单数据
        testOrder = new TbOrder();
        testOrder.setOrderId(123456789L);
        testOrder.setUserId("test_user_001");
        testOrder.setPaymentType("1");
        testOrder.setStatus("1");
        testOrder.setReceiver("张三");
        testOrder.setReceiverMobile("13800138000");
        testOrder.setReceiverAreaName("北京市朝阳区");
        testOrder.setPayment(new BigDecimal("299.99"));

        // 准备测试支付日志数据
        testPayLog = new TbPayLog();
        testPayLog.setOutTradeNo("PAY123456");
        testPayLog.setUserId("test_user_001");
        testPayLog.setTotalFee(29999L);
        testPayLog.setTradeState("0");

        // Mock IdWorker
        Mockito.when(idWorker.nextId()).thenReturn(123456789L, 987654321L, 555666777L);
    }

    /**
     * 测试查询全部订单
     */
    @Test
    public void testFindAll() {
        // 准备测试数据
        List<TbOrder> orderList = new ArrayList<>();
        orderList.add(testOrder);

        // Mock行为
        Mockito.when(orderMapper.selectByExample(null)).thenReturn(orderList);

        // 执行测试
        List<TbOrder> result = orderService.findAll();

        // 验证结果
        assertNotNull("订单列表不应为null", result);
        assertEquals("订单数量应为1", 1, result.size());
        assertEquals("订单ID不匹配", testOrder.getOrderId(), result.get(0).getOrderId());

        // 验证方法调用
        Mockito.verify(orderMapper).selectByExample(null);
    }

    /**
     * 测试按ID查询订单
     */
    @Test
    public void testFindOne() {
        Long orderId = 123456789L;

        // Mock行为
        Mockito.when(orderMapper.selectByPrimaryKey(orderId)).thenReturn(testOrder);

        // 执行测试
        TbOrder result = orderService.findOne(orderId);

        // 验证结果
        assertNotNull("订单不应为null", result);
        assertEquals("订单ID不匹配", orderId, result.getOrderId());

        // 验证方法调用
        Mockito.verify(orderMapper).selectByPrimaryKey(orderId);
    }

    /**
     * 测试查询不存在的订单
     */
    @Test(expected = ResourceNotFoundException.class)
    public void testFindOneNotFound() {
        Long orderId = 999999L;

        // Mock行为：返回null
        Mockito.when(orderMapper.selectByPrimaryKey(orderId)).thenReturn(null);

        // 执行测试（应该抛出异常）
        orderService.findOne(orderId);
    }

    /**
     * 测试支付状态更新
     */
    @Test
    public void testUpdateOrderStatus() {
        String outTradeNo = "PAY123456";
        String transactionId = "WXTRANSACTION123";

        // Mock支付日志 - 确保金额一致（元转分：299.99 * 100 = 29999）
        testOrder.setPayment(new BigDecimal("299.99"));
        testPayLog.setTotalFee(29999L); // 299.99元 = 29999分

        Mockito.when(payLogMapper.selectByPrimaryKey(outTradeNo)).thenReturn(testPayLog);

        // Mock订单列表
        List<TbOrder> orderList = new ArrayList<>();
        orderList.add(testOrder);
        Mockito.when(orderMapper.selectByPrimaryKey(testOrder.getOrderId())).thenReturn(testOrder);

        // Mock Redis - 使用HashOperations mock避免NPE
        org.springframework.data.redis.core.HashOperations hashOps =
            Mockito.mock(org.springframework.data.redis.core.HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("payLog")).thenReturn(hashOps);
        Mockito.when(hashOps.get(testPayLog.getUserId())).thenReturn(testPayLog);
        Mockito.doNothing().when(hashOps).delete(Mockito.anyString());

        // 执行测试
        orderService.updateOrderStatus(outTradeNo, transactionId);

        // 验证结果
        Mockito.verify(payLogMapper).selectByPrimaryKey(outTradeNo);
        Mockito.verify(payLogMapper).updateByPrimaryKey(testPayLog);
        Mockito.verify(orderMapper).updateByPrimaryKey(Mockito.any(TbOrder.class));
        Mockito.verify(redisTemplate).boundHashOps("payLog");

        // 验证支付日志状态已更新
        assertEquals("支付状态应为已支付", "1", testPayLog.getTradeState());
        assertEquals("交易流水号应匹配", transactionId, testPayLog.getTransactionId());
    }

    /**
     * 测试查询支付日志（Redis不存在）
     */
    @Test
    public void testSearchPayLogFromRedisNotFound() {
        String userId = "test_user_001";

        // Mock Redis返回null
        Mockito.when(redisTemplate.boundHashOps("payLog").get(userId)).thenReturn(null);

        // 执行测试
        TbPayLog result = orderService.searchPayLogFromRedis(userId);

        // 验证结果
        assertNull("支付日志应为null", result);
    }

    /**
     * 测试查询支付日志（Redis存在）
     */
    @Test
    public void testSearchPayLogFromRedisFound() {
        String userId = "test_user_001";

        // Mock Redis返回支付日志
        Mockito.when(redisTemplate.boundHashOps("payLog").get(userId)).thenReturn(testPayLog);

        // 执行测试
        TbPayLog result = orderService.searchPayLogFromRedis(userId);

        // 验证结果
        assertNotNull("支付日志不应为null", result);
        assertEquals("支付日志ID不匹配", testPayLog.getOutTradeNo(), result.getOutTradeNo());
    }

    /**
     * 测试批量删除订单
     */
    @Test
    public void testDelete() {
        Long[] ids = {123456789L, 987654321L};

        // 执行测试
        orderService.delete(ids);

        // 验证方法调用
        Mockito.verify(orderMapper, Mockito.times(2)).deleteByPrimaryKey(Mockito.anyLong());
    }

    /**
     * 测试删除空ID数组
     */
    @Test
    public void testDelete_EmptyArray() {
        Long[] ids = {};

        // 执行测试（不应该抛出异常）
        orderService.delete(ids);

        // 验证没有任何删除操作
        Mockito.verify(orderMapper, Mockito.never()).deleteByPrimaryKey(Mockito.anyLong());
    }

    /**
     * 测试金额计算精度
     */
    @Test
    public void testAmountCalculationPrecision() {
        // 测试BigDecimal金额计算的精确性
        BigDecimal price1 = new BigDecimal("19.99");
        BigDecimal price2 = new BigDecimal("29.99");
        BigDecimal price3 = new BigDecimal("9.99");

        // 使用BigDecimal累加
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(price1);
        total = total.add(price2);
        total = total.add(price3);

        // 预期结果：59.97
        BigDecimal expected = new BigDecimal("59.97");
        assertEquals("金额计算应精确", 0, total.compareTo(expected));

        // 验证元转分
        Long totalFee = total.multiply(new BigDecimal(100))
                          .setScale(0, BigDecimal.ROUND_HALF_UP)
                          .longValue();
        assertEquals("元转分应正确", 5997L, totalFee.longValue());
    }

    /**
     * 测试金额为0的订单（边界条件）
     */
    @Test
    public void testAmountCalculation_Zero() {
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal total = price.add(new BigDecimal("0.00"));

        assertEquals("零金额应正确", 0, total.compareTo(BigDecimal.ZERO));

        Long totalFee = total.multiply(new BigDecimal(100))
                          .setScale(0, BigDecimal.ROUND_HALF_UP)
                          .longValue();
        assertEquals("零金额转分应为0", 0L, totalFee.longValue());
    }
}

    // ========== 补充的关键业务流程测试 ==========

    /**
     * 测试findPage分页查询
     */
    @Test
    public void testFindPage() {
        int pageNum = 1;
        int pageSize = 10;

        // Mock PageHelper
        com.github.pagehelper.Page<TbOrder> page = new com.github.pagehelper.Page<>(pageNum, pageSize);
        List<TbOrder> orderList = new ArrayList<>();
        orderList.add(testOrder);
        page.addAll(orderList);
        page.setTotal(1);

        Mockito.when(orderMapper.selectByExample(Mockito.any(TbOrderExample.class))).thenReturn(orderList);

        // 执行测试
        entity.PageResult result = orderService.findPage(pageNum, pageSize);

        // 验证结果
        assertNotNull("分页结果不应为null", result);
        assertNotNull("商品列表不应为null", result.getRows());
        assertEquals("总记录数应为1", 1L, result.getTotal());
        assertEquals("当前页商品数应为1", 1, result.getRows().size());
    }

    /**
     * 测试add方法参数校验（订单信息为空）
     */
    @Test(expected = com.pinyougou.exception.ValidationException.class)
    public void testAdd_NullOrder() {
        orderService.add(null);
    }

    /**
     * 测试add方法参数校验（用户ID为空）
     */
    @Test(expected = com.pinyougou.exception.ValidationException.class)
    public void testAdd_EmptyUserId() {
        TbOrder order = new TbOrder();
        order.setUserId(""); // 空字符串
        order.setPaymentType("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");

        orderService.add(order);
    }

    /**
     * 测试update方法（订单状态更新）
     */
    @Test
    public void testUpdate() {
        TbOrder updateOrder = new TbOrder();
        updateOrder.setOrderId(123456789L);
        updateOrder.setStatus("2"); // 更新为已付款

        Mockito.when(orderMapper.selectByPrimaryKey(123456789L)).thenReturn(testOrder);
        Mockito.doNothing().when(orderMapper).updateByPrimaryKey(Mockito.any(TbOrder.class));

        orderService.update(updateOrder);

        Mockito.verify(orderMapper).updateByPrimaryKey(updateOrder);
    }

    /**
     * 测试findOne（订单不存在）
     */
    @Test(expected = com.pinyougou.exception.ResourceNotFoundException.class)
    public void testFindOne_NotFound() {
        Mockito.when(orderMapper.selectByPrimaryKey(999999L)).thenReturn(null);
        orderService.findOne(999999L);
    }
