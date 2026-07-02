package com.pinyougou.order.integration;

import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.order.service.OrderService;
import org.junit.Ignore;
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
 * <p>
 * 运行前提：
 * - 需要配置数据库Schema（tb_order、tb_pay_log表）
 * - 需要配置Spring上下文
 * - 在没有数据库环境下，测试将被@Ignore跳过
 * <p>
 * 事务说明：
 * - 继承IntegrationTestBase，使用@Transactional注解
 * - 每个测试方法运行在独立事务中，测试结束后自动回滚
 * - 不会污染数据库数据
 * <p>
 * 测试数据：
 * - 用户ID: test_user_001
 * - 支付方式: 1（在线支付）
 * - 支付金额: 299.99元
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
@Ignore("需要数据库环境，CI环境跳过")
public class OrderServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private TbPayLogMapper payLogMapper;

    /**
     * 测试订单创建和查询
     * <p>
     * 验证：
     * 1. 订单能成功插入数据库
     * 2. 插入后能正确查询到
     * 3. 用户ID、支付金额等字段正确
     * 4. tb_order表存在
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

        // 验证插入结果
        assertNotNull("订单不应为null", found);
        assertEquals("用户ID应匹配", order.getUserId(), found.getUserId());
        assertEquals("支付金额应匹配", 0, order.getPayment().compareTo(found.getPayment()));

        // 验证数据库表存在
        assertTrue("tb_order表应存在", tableExists("tb_order"));
    }

    /**
     * 测试订单更新
     * <p>
     * 验证：
     * 1. 订单状态能正确更新
     * 2. 更新后查询到的是最新状态
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

        // 更新订单状态为已付款
        order.setStatus("2");
        orderMapper.updateByPrimaryKey(order);

        // 验证更新结果
        TbOrder updated = orderMapper.selectByPrimaryKey(order.getOrderId());
        assertEquals("状态应已更新", "2", updated.getStatus());
    }

    /**
     * 测试订单删除
     * <p>
     * 验证：
     * 1. 订单能成功删除
     * 2. 删除后查询返回null
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

        // 验证删除结果
        TbOrder deleted = orderMapper.selectByPrimaryKey(orderId);
        assertNull("订单应已删除", deleted);
    }

    /**
     * 测试支付日志创建和查询
     * <p>
     * 验证：
     * 1. 支付日志能成功插入
     * 2. 插入后能正确查询
     * 3. 各字段值正确
     * 4. tb_pay_log表存在
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

        // 验证插入结果
        assertNotNull("支付日志不应为null", found);
        assertEquals("交易流水号应匹配", payLog.getOutTradeNo(), found.getOutTradeNo());
        assertEquals("用户ID应匹配", payLog.getUserId(), found.getUserId());
        assertEquals("金额应匹配", payLog.getTotalFee(), found.getTotalFee());

        // 验证数据库表存在
        assertTrue("tb_pay_log表应存在", tableExists("tb_pay_log"));
    }

    /**
     * 测试事务回滚
     * <p>
     * 验证：
     * 1. 插入操作在事务中执行
     * 2. 测试结束后事务自动回滚
     * <p>
     * 注意：由于@Transactional注解，测试方法结束后事务会回滚
     * 此测试主要用于演示事务回滚的概念
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

        // 验证已插入（事务未提交前可见）
        assertEquals("应增加1条记录", initialCount + 1, countTableRows("tb_order"));

        // 测试方法执行完后会自动回滚，验证回滚后数据
        // 注意：这里实际上不会执行到，因为@Transactional在方法结束后才回滚
        // 这个测试主要用于演示事务回滚的概念
    }

    /**
     * 测试数据库约束（如果有）
     * <p>
     * 验证：
     * 1. tb_order表存在
     * 2. tb_pay_log表存在
     * <p>
     * 扩展：可以添加更多表结构验证，如字段类型、约束等
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
