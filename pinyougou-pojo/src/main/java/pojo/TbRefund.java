package com.pinyougou.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class TbRefund implements Serializable {
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 退款金额
     */
    private BigDecimal refundFee;

    /**
     * 退款原因：1-买家取消订单 2-买家退货退款 3-卖家同意退款 4-系统自动退款
     */
    private String reason;

    /**
     * 退款状态：0-待处理 1-退款成功 2-退款失败
     */
    private String status;

    /**
     * 交易流水号
     */
    private String transactionId;

    /**
     * 退款响应内容
     */
    private String responseContent;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 退款完成时间
     */
    private Date finishTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getRefundFee() {
        return refundFee;
    }

    public void setRefundFee(BigDecimal refundFee) {
        this.refundFee = refundFee;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason == null ? null : reason.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId == null ? null : transactionId.trim();
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent == null ? null : responseContent.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }
}
