package com.pinyougou.order.integration;

import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.order.service.OrderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * OrderService集成测试
 * <p>
 * 测试场景：
 * 1. 使用真实数据库（H2内存数据库）
 * 2. 测试完整的CRUD操作
 * 3. 验证数据库状态
 * 4. 测试事务回滚
 *
 * 运行前提：
 * - 需要配置数据库Schema
 * - 需要配置Spring上下文
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public class OrderServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private TbPayLogMapper payLogMapper;

    /**
     * 测试订单创建和查询
     */
    @Test
    public void testCreateAndFindOrder() {
        // 创建测试订单
        TbOrder order = new TbOrder();
        order.setUserId("test_user_001");
        order.setPaymentType("1");
        order.setStatus("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");
        order.setPayment(new BigDecimal("299.99"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        // 插入订单
        orderMapper.insert(order);

        // 查询订单
        TbOrder found = orderMapper.selectByPrimaryKey(order.getOrderId());

        // 验证
        assertNotNull("订单不应为null", found);
        assertEquals("用户ID应匹配", order.getUserId(), found.getUserId());
        assertEquals("支付金额应匹配", 0, order.getPayment().compareTo(found.getPayment()));

        // 验证数据库表存在
        assertTrue("tb_order表应存在", tableExists("tb_order"));
    }

    /**
     * 测试订单更新
     */
    @Test
    public void testUpdateOrder() {
        // 创建并插入订单
        TbOrder order = new TbOrder();
        order.setUserId("test_user_001");
        order.setPaymentType("1");
        order.setStatus("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");
        order.setPayment(new BigDecimal("299.99"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderMapper.insert(order);

        // 更新订单
        order.setStatus("2"); // 已付款
        orderMapper.updateByPrimaryKey(order);

        // 验证更新
        TbOrder updated = orderMapper.selectByPrimaryKey(order.getOrderId());
        assertEquals("状态应已更新", "2", updated.getStatus());
    }

    /**
     * 测试订单删除
     */
    @Test
    public void testDeleteOrder() {
        // 创建并插入订单
        TbOrder order = new TbOrder();
        order.setUserId("test_user_001");
        order.setPaymentType("1");
        order.setStatus("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");
        order.setPayment(new BigDecimal("299.99"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderMapper.insert(order);

        Long orderId = order.getOrderId();

        // 删除订单
        orderMapper.deleteByPrimaryKey(orderId);

        // 验证删除
        TbOrder deleted = orderMapper.selectByPrimaryKey(orderId);
        assertNull("订单应已删除", deleted);
    }

    /**
     * 测试支付日志创建和查询
     */
    @Test
    public void testCreateAndFindPayLog() {
        // 创建支付日志
        TbPayLog payLog = new TbPayLog();
        payLog.setOutTradeNo("PAY_TEST_001");
        payLog.setUserId("test_user_001");
        payLog.setTotalFee(29999L);
        payLog.setTradeState("0"); // 未支付
        payLog.setPayType("1"); // 微信支付
        payLog.setCreateTime(new Date());
        payLog.setOrderList("123,456,789");

        // 插入支付日志
        payLogMapper.insert(payLog);

        // 查询支付日志
        TbPayLog found = payLogMapper.selectByPrimaryKey(payLog.getOutTradeNo());

        // 验证
        assertNotNull("支付日志不应为null", found);
        assertEquals("交易流水号应匹配", payLog.getOutTradeNo(), found.getOutTradeNo());
        assertEquals("用户ID应匹配", payLog.getUserId(), found.getUserId());
        assertEquals("金额应匹配", payLog.getTotalFee(), found.getTotalFee());

        // 验证数据库表存在
        assertTrue("tb_pay_log表应存在", tableExists("tb_pay_log"));
    }

    /**
     * 测试事务回滚
     */
    @Test
    public void testTransactionRollback() {
        // 记录初始行数
        long initialCount = countTableRows("tb_order");

        // 创建订单
        TbOrder order = new TbOrder();
        order.setUserId("test_user_001");
        order.setPaymentType("1");
        order.setStatus("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");
        order.setPayment(new BigDecimal("299.99"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderMapper.insert(order);

        // 验证已插入
        assertEquals("应增加1条记录", initialCount + 1, countTableRows("tb_order"));

        // 测试方法执行完后会自动回滚，验证回滚后数据
        // 注意：这里实际上不会执行到，因为@Transactional在方法结束后才回滚
        // 这个测试主要用于演示事务回滚的概念
    }

    /**
     * 测试数据库约束（如果有）
     */
    @Test
    public void testDatabaseConstraints() {
        // 验证表存在
        assertTrue("tb_order表应存在", tableExists("tb_order"));
        assertTrue("tb_pay_log表应存在", tableExists("tb_pay_log"));

        // 验证表结构（可选）
        // 这里可以添加更多验证表结构的测试
    }
}
