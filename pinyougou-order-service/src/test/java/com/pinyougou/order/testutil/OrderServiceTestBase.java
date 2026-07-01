package com.pinyougou.order.testutil;

import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbPayLog;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OrderServiceImpl测试工具类
 * <p>
 * 提供订单服务测试的公共方法：
 * 1. 创建测试用的订单数据
 * 2. 创建测试用的支付日志数据
 * 3. Mock Redis和Mapper的常用操作
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class OrderServiceTestBase {

    /**
     * 创建测试订单（默认数据）
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
        order.setAddress("北京市朝阳区xxx街道");
        order.setPostFee(new BigDecimal("0.00"));
        order.setPayment(new BigDecimal("299.99"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        return order;
    }

    /**
     * 创建测试订单（自定义数据）
     *
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param paymentType 支付方式
     * @param status 订单状态
     * @param payment 支付金额
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
     *
     * @return TbPayLog对象
     */
    protected TbPayLog createTestPayLog() {
        TbPayLog payLog = new TbPayLog();
        payLog.setOutTradeNo("PAY123456");
        payLog.setUserId("test_user_001");
        payLog.setTotalFee(29999L); // 299.99元 = 29999分
        payLog.setTradeState("0"); // 未支付
        payLog.setPayType("1"); // 微信支付
        payLog.setCreateTime(new Date());
        return payLog;
    }

    /**
     * 创建测试支付日志（自定义数据）
     *
     * @param outTradeNo 交易流水号
     * @param userId 用户ID
     * @param totalFee 支付金额（分）
     * @param tradeState 交易状态
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
     * 创建批量订单ID列表
     *
     * @param count 数量
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
     * Mock Redis HashOperations
     *
     * @param redisTemplate RedisTemplate
     * @return HashOperations mock对象
     */
    protected org.springframework.data.redis.core.HashOperations mockHashOperations(RedisTemplate<String, Object> redisTemplate) {
        org.springframework.data.redis.core.HashOperations hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(hashOps);
        return hashOps;
    }

    /**
     * 验证金额计算精度（元转分）
     *
     * @param yuanAmount 金额（元）
     * @param expectedFen 预期的金额（分）
     */
    protected void verifyAmountConversion(BigDecimal yuanAmount, long expectedFen) {
        Long actualFen = yuanAmount.multiply(new BigDecimal(100))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .longValue();
        assertEquals("金额转换应正确", expectedFen, actualFen.longValue());
    }
}
