package com.pinyougou.order.testutil;

import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbPayLog;
import org.mockito.Mockito;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 订单服务测试基类
 * <p>
 * 功能说明：
 * - 提供测试订单和支付日志数据的创建方法
 * - 提供Redis操作的Mock方法
 * - 提供金额转换的验证方法
 * <p>
 * 使用方式：
 * - 子类继承此类，可直接调用提供的工具方法
 * - 所有方法均为protected，仅对子类可见
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class OrderServiceTestBase {

    /**
     * 创建测试订单（默认数据）
     * <p>
     * 默认值：
     * - 订单ID: 123456789
     * - 用户ID: test_user_001
     * - 支付方式: 1（在线支付）
     * - 状态: 1（未付款）
     * - 收货人: 张三
     * - 手机号: 13800138000
     * - 地址: 北京市朝阳区
     * - 金额: 299.99元
     *
     * @return TbOrder对象
     */
    protected TbOrder createTestOrder() {
        TbOrder order = new TbOrder();
        order.setOrderId(123456789L);
        order.setUserId("test_user_001");
        order.setPaymentType("1");
        order.setStatus("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");
        order.setPostFee("0.00");
        order.setPayment(new BigDecimal("299.99"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        return order;
    }

    /**
     * 创建测试订单（自定义数据）
     *
     * @param orderId     订单ID
     * @param userId      用户ID
     * @param paymentType 支付方式（"1"-在线支付，"2"-货到付款）
     * @param status      状态（"1"-未付款，"2"-已付款，"3"-已取消）
     * @param payment     支付金额（元）
     * @return TbOrder对象
     */
    protected TbOrder createTestOrder(Long orderId, String userId, String paymentType,
                                       String status, BigDecimal payment) {
        TbOrder order = new TbOrder();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setPaymentType(paymentType);
        order.setStatus(status);
        order.setReceiver("测试收货人");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");
        order.setPayment(payment);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        return order;
    }

    /**
     * 创建测试支付日志（默认数据）
     * <p>
     * 默认值：
     * - 流水号: PAY123456
     * - 用户ID: test_user_001
     * - 金额: 29999分（299.99元）
     * - 交易状态: 0（未支付）
     * - 支付类型: 1（微信支付）
     *
     * @return TbPayLog对象
     */
    protected TbPayLog createTestPayLog() {
        TbPayLog payLog = new TbPayLog();
        payLog.setOutTradeNo("PAY123456");
        payLog.setUserId("test_user_001");
        payLog.setTotalFee(29999L);
        payLog.setTradeState("0");
        payLog.setPayType("1");
        payLog.setCreateTime(new Date());
        return payLog;
    }

    /**
     * 创建测试支付日志（自定义数据）
     *
     * @param outTradeNo  交易流水号
     * @param userId      用户ID
     * @param totalFee    金额（分）
     * @param tradeState  交易状态（"0"-未支付，"1"-已支付，"2"-已关闭）
     * @return TbPayLog对象
     */
    protected TbPayLog createTestPayLog(String outTradeNo, String userId, Long totalFee, String tradeState) {
        TbPayLog payLog = new TbPayLog();
        payLog.setOutTradeNo(outTradeNo);
        payLog.setUserId(userId);
        payLog.setTotalFee(totalFee);
        payLog.setTradeState(tradeState);
        payLog.setPayType("1");
        payLog.setCreateTime(new Date());
        return payLog;
    }

    /**
     * 创建订单ID列表
     * <p>
     * 用于测试批量操作（如updateOrderStatus）
     *
     * @param count 订单数量
     * @return 订单ID列表
     */
    protected List<String> createOrderIdList(int count) {
        List<String> orderIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orderIds.add(String.valueOf(123456789L + i));
        }
        return orderIds;
    }

    /**
     * Mock Redis Hash操作
     * <p>
     * 创建BoundHashOperations mock，并设置redisTemplate.boundHashOps返回该mock
     * <p>
     * 使用场景：测试需要Redis Hash操作的方法（如searchPayLogFromRedis）
     *
     * @param redisTemplate RedisTemplate对象
     * @return BoundHashOperations mock对象
     */
    @SuppressWarnings("unchecked")
    protected BoundHashOperations<String, Object, Object> mockBoundHashOperations(
            RedisTemplate<String, Object> redisTemplate) {
        BoundHashOperations<String, Object, Object> boundHashOps =
            mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(boundHashOps);
        return boundHashOps;
    }

    /**
     * 验证金额元转分转换
     * <p>
     * 验证：元金额 * 100 = 分金额
     * <p>
     * 转换规则：
     * - 乘以100
     * - 四舍五入取整（HALF_UP）
     * - 转换为long类型
     *
     * @param yuanAmount  元金额
     * @param expectedFen 预期分金额
     */
    protected void verifyAmountConversion(BigDecimal yuanAmount, long expectedFen) {
        Long actualFen = yuanAmount.multiply(new BigDecimal(100))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .longValue();
        assertEquals("金额转换应正确", Long.valueOf(expectedFen), actualFen);
    }
}
