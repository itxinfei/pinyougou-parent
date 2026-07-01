package com.pinyougou.order.service;

import java.util.Map;

import com.pinyougou.pojo.TbOrder;

/**
 * 退款服务接口
 *
 * @author Administrator
 */
public interface RefundService {

    /**
     * 订单取消
     *
     * @param orderId 订单ID
     * @param reason 取消原因
     * @return 取消结果
     */
    Map<String, Object> cancelOrder(Long orderId, String reason);

    /**
     * 退款申请
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     * @param refundFee 退款金额
     * @return 退款结果
     */
    Map<String, Object> applyRefund(Long orderId, String reason, Double refundFee);

    /**
     * 确认退款（审核通过）
     *
     * @param orderId 订单ID
     * @return 退款结果
     */
    Map<String, Object> confirmRefund(Long orderId);

    /**
     * 拒绝退款
     *
     * @param orderId 订单ID
     * @param reason 拒绝原因
     * @return 操作结果
     */
    Map<String, Object> rejectRefund(Long orderId, String reason);

    /**
     * 查询退款记录
     *
     * @param orderId 订单ID
     * @return 退款记录
     */
    Map<String, Object> findRefundByOrderId(Long orderId);
}
