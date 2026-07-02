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

public abstract class OrderServiceTestBase {

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

    protected List<String> createOrderIdList(int count) {
        List<String> orderIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orderIds.add(String.valueOf(123456789L + i));
        }
        return orderIds;
    }

    @SuppressWarnings("unchecked")
    protected BoundHashOperations<String, Object, Object> mockBoundHashOperations(
            RedisTemplate<String, Object> redisTemplate) {
        BoundHashOperations<String, Object, Object> boundHashOps =
            mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(boundHashOps);
        return boundHashOps;
    }

    protected void verifyAmountConversion(BigDecimal yuanAmount, long expectedFen) {
        Long actualFen = yuanAmount.multiply(new BigDecimal(100))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .longValue();
        assertEquals("金额转换应正确", Long.valueOf(expectedFen), actualFen);
    }
}
