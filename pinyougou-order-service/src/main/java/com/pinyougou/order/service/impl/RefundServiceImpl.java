package com.pinyougou.order.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbRefundMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbOrderItemExample;
import com.pinyougou.pojo.TbRefund;
import com.pinyougou.pojo.TbRefundExample;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.order.service.RefundService;

import entity.Result;
import util.IdWorker;

/**
 * 退款服务实现类
 *
 * @author Administrator
 */
@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger logger = Logger.getLogger(RefundServiceImpl.class);

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private TbOrderItemMapper orderItemMapper;

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private TbRefundMapper refundMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private IdWorker idWorker;

    // 微信支付配置（实际应该从配置文件读取）
    private static final String APP_ID = "wx8888888888888888";
    private static final String MCH_ID = "1900000109";
    private static final String KEY = "192006250b4c09247ec02edce69f6a2d";
    private static final String REFUND_URL = "https://api.mch.weixin.qq.com/secapi/pay/refund";

    /**
     * 订单取消
     */
    @Override
    @Transactional
    public Map<String, Object> cancelOrder(Long orderId, String reason) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 查询订单
            TbOrder order = orderMapper.selectByPrimaryKey(orderId);
            if (order == null) {
                resultMap.put("success", false);
                resultMap.put("message", "订单不存在");
                return resultMap;
            }

            // 2. 检查订单状态
            String status = order.getStatus();
            if ("TRADE_CLOSED".equals(status) || "TRADE_FINISHED".equals(status)) {
                resultMap.put("success", false);
                resultMap.put("message", "订单状态不允许取消");
                return resultMap;
            }

            // 3. 根据订单状态处理
            if ("TRADE_SUCCESS".equals(status)) {
                // 已付款，需要退款
                resultMap.put("success", false);
                resultMap.put("message", "订单已付款，请申请退款");
                return resultMap;
            } else {
                // 未付款，直接取消
                // 3.1 更新订单状态
                order.setStatus("TRADE_CLOSED");
                order.setCloseTime(new Date());
                order.setCancelReason(reason);
                orderMapper.updateByPrimaryKey(order);

                // 3.2 恢复库存
                restoreStock(orderId);

                // 3.3 退还优惠券
                returnCoupon(orderId);

                resultMap.put("success", true);
                resultMap.put("message", "订单取消成功");
                logger.info("订单取消成功: " + orderId + ", 原因: " + reason);
            }

        } catch (Exception e) {
            logger.error("订单取消失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "订单取消失败：" + e.getMessage());
        }

        return resultMap;
    }

    /**
     * 退款申请
     */
    @Override
    @Transactional
    public Map<String, Object> applyRefund(Long orderId, String reason, Double refundFee) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 查询订单
            TbOrder order = orderMapper.selectByPrimaryKey(orderId);
            if (order == null) {
                resultMap.put("success", false);
                resultMap.put("message", "订单不存在");
                return resultMap;
            }

            // 2. 检查订单状态
            if (!"TRADE_SUCCESS".equals(order.getStatus())) {
                resultMap.put("success", false);
                resultMap.put("message", "只有已付款订单才能申请退款");
                return resultMap;
            }

            // 3. 检查是否已申请过退款
            TbRefundExample example = new TbRefundExample();
            TbRefundExample.Criteria criteria = example.createCriteria();
            criteria.andOrderIdEqualTo(orderId);

            long count = refundMapper.countByExample(example);
            if (count > 0) {
                resultMap.put("success", false);
                resultMap.put("message", "该订单已申请过退款");
                return resultMap;
            }

            // 4. 验证退款金额
            BigDecimal payment = order.getPayment();
            BigDecimal requestFee = BigDecimal.valueOf(refundFee);
            if (requestFee.compareTo(payment) > 0) {
                resultMap.put("success", false);
                resultMap.put("message", "退款金额不能大于订单金额");
                return resultMap;
            }

            // 5. 创建退款记录
            TbRefund refund = new TbRefund();
            refund.setId(idWorker.nextId());
            refund.setOrderId(orderId);
            refund.setRefundFee(requestFee);
            refund.setReason(reason);
            refund.setStatus("0"); // 待处理
            refund.setCreateTime(new Date());

            refundMapper.insert(refund);

            // 6. 更新订单状态为退款中
            order.setStatus("REFUND_APPLY");
            order.setCancelReason(reason);
            orderMapper.updateByPrimaryKey(order);

            resultMap.put("success", true);
            resultMap.put("message", "退款申请提交成功，等待审核");
            logger.info("退款申请成功: " + orderId + ", 金额: " + refundFee + ", 原因: " + reason);

        } catch (Exception e) {
            logger.error("退款申请失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "退款申请失败：" + e.getMessage());
        }

        return resultMap;
    }

    /**
     * 确认退款（审核通过）
     */
    @Override
    @Transactional
    public Map<String, Object> confirmRefund(Long orderId) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 查询订单
            TbOrder order = orderMapper.selectByPrimaryKey(orderId);
            if (order == null) {
                resultMap.put("success", false);
                resultMap.put("message", "订单不存在");
                return resultMap;
            }

            // 2. 查询退款记录
            TbRefundExample example = new TbRefundExample();
            TbRefundExample.Criteria criteria = example.createCriteria();
            criteria.andOrderIdEqualTo(orderId);
            criteria.andStatusEqualTo("0"); // 待处理

            List<TbRefund> refundList = refundMapper.selectByExample(example);
            if (refundList == null || refundList.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("message", "未找到待处理的退款记录");
                return resultMap;
            }

            TbRefund refund = refundList.get(0);

            // 3. 调用微信退款接口
            Map<String, Object> refundResult = weixinRefund(order, refund.getRefundFee().doubleValue());

            boolean success = (boolean) refundResult.get("success");
            if (!success) {
                resultMap.put("success", false);
                resultMap.put("message", refundResult.get("message"));
                return resultMap;
            }

            // 4. 更新退款记录
            refund.setStatus("1"); // 退款成功
            refund.setTransactionId((String) refundResult.get("transactionId"));
            refund.setResponseContent(JSON.toJSONString(refundResult.get("response")));
            refund.setFinishTime(new Date());
            refundMapper.updateByPrimaryKey(refund);

            // 5. 更新订单状态
            order.setStatus("TRADE_CLOSED");
            order.setCloseTime(new Date());
            orderMapper.updateByPrimaryKey(order);

            // 6. 恢复库存
            restoreStock(orderId);

            // 7. 退还优惠券
            returnCoupon(orderId);

            resultMap.put("success", true);
            resultMap.put("message", "退款成功");
            logger.info("退款成功: " + orderId + ", 金额: " + refund.getRefundFee());

        } catch (Exception e) {
            logger.error("退款失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "退款失败：" + e.getMessage());
        }

        return resultMap;
    }

    /**
     * 拒绝退款
     */
    @Override
    @Transactional
    public Map<String, Object> rejectRefund(Long orderId, String reason) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 查询订单
            TbOrder order = orderMapper.selectByPrimaryKey(orderId);
            if (order == null) {
                resultMap.put("success", false);
                resultMap.put("message", "订单不存在");
                return resultMap;
            }

            // 2. 查询退款记录
            TbRefundExample example = new TbRefundExample();
            TbRefundExample.Criteria criteria = example.createCriteria();
            criteria.andOrderIdEqualTo(orderId);
            criteria.andStatusEqualTo("0");

            List<TbRefund> refundList = refundMapper.selectByExample(example);
            if (refundList == null || refundList.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("message", "未找到待处理的退款记录");
                return resultMap;
            }

            TbRefund refund = refundList.get(0);

            // 3. 更新退款记录
            refund.setStatus("2"); // 退款失败
            refund.setResponseContent("拒绝原因：" + reason);
            refund.setFinishTime(new Date());
            refundMapper.updateByPrimaryKey(refund);

            // 4. 恢复订单状态
            order.setStatus("TRADE_SUCCESS");
            orderMapper.updateByPrimaryKey(order);

            resultMap.put("success", true);
            resultMap.put("message", "已拒绝退款申请");
            logger.info("拒绝退款: " + orderId + ", 原因: " + reason);

        } catch (Exception e) {
            logger.error("拒绝退款失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "操作失败：" + e.getMessage());
        }

        return resultMap;
    }

    /**
     * 查询退款记录
     */
    @Override
    public Map<String, Object> findRefundByOrderId(Long orderId) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            TbRefundExample example = new TbRefundExample();
            TbRefundExample.Criteria criteria = example.createCriteria();
            criteria.andOrderIdEqualTo(orderId);

            List<TbRefund> refundList = refundMapper.selectByExample(example);
            if (refundList == null || refundList.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("message", "未找到退款记录");
                return resultMap;
            }

            TbRefund refund = refundList.get(0);

            resultMap.put("success", true);
            resultMap.put("refund", refund);

        } catch (Exception e) {
            logger.error("查询退款记录失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "查询失败");
        }

        return resultMap;
    }

    /**
     * 恢复库存
     */
    private void restoreStock(Long orderId) {
        // 查询订单项
        TbOrderItemExample itemExample = new TbOrderItemExample();
        TbOrderItemExample.Criteria itemCriteria = itemExample.createCriteria();
        itemCriteria.andOrderIdEqualTo(orderId);

        List<TbOrderItem> orderItems = orderItemMapper.selectByExample(itemExample);

        // 恢复库存
        for (TbOrderItem orderItem : orderItems) {
            itemMapper.restoreStockCount(orderItem.getItemId(), orderItem.getNum());
            logger.info("恢复库存: 商品ID=" + orderItem.getItemId() + ", 数量=" + orderItem.getNum());
        }
    }

    /**
     * 退还优惠券
     */
    private void returnCoupon(Long orderId) {
        try {
            // 查询订单使用的优惠券
            // TODO: 需要在TbOrder中添加user_coupon_id字段
            // 暂时注释
            /*
            TbOrder order = orderMapper.selectByPrimaryKey(orderId);
            Long userCouponId = order.getUserCouponId();
            if (userCouponId != null) {
                // 退还优惠券
                TbUserCoupon userCoupon = userCouponService.findOne(userCouponId);
                userCoupon.setStatus("0"); // 未使用
                userCouponService.update(userCoupon);
                logger.info("退还优惠券: " + userCouponId);
            }
            */
        } catch (Exception e) {
            logger.error("退还优惠券失败", e);
        }
    }

    /**
     * 调用微信退款接口（模拟实现）
     */
    private Map<String, Object> weixinRefund(TbOrder order, Double refundFee) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // TODO: 实际应该调用微信支付退款API
            // 这里模拟退款成功

            // 模拟响应
            resultMap.put("success", true);
            resultMap.put("transactionId", "WXREFUND" + System.currentTimeMillis());
            resultMap.put("response", "{\"return_code\":\"SUCCESS\"}");

            logger.info("微信退款成功: 订单ID=" + order.getOrderId() + ", 金额=" + refundFee);

        } catch (Exception e) {
            logger.error("微信退款失败", e);
            resultMap.put("success", false);
            resultMap.put("message", "微信退款失败：" + e.getMessage());
        }

        return resultMap;
    }
}
