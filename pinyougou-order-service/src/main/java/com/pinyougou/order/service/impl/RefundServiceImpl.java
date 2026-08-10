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
import com.pinyougou.exception.InsufficientStockException;
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

    // ========== 微信支付配置（从配置文件读取） ==========
    @org.springframework.beans.factory.annotation.Value("${weixin.appid:}")
    private String appId;
    @org.springframework.beans.factory.annotation.Value("${weixin.mch.id:}")
    private String mchId;
    @org.springframework.beans.factory.annotation.Value("${weixin.key:}")
    private String key;
    @org.springframework.beans.factory.annotation.Value("${weixin.refund.url:https://api.mch.weixin.qq.com/secapi/pay/refund}")
    private String refundUrl;

    /**
     * 订单取消功能
     * <p>
     * 业务规则：
     * - 仅允许取消未付款订单（状态为 "1"）
     * - 已付款订单需要走退款流程，不能直接取消
     * - 已关闭/已完成的订单不能取消
     * <p>
     * 执行步骤：
     * 1. 验证订单存在性和状态合法性
     * 2. 更新订单状态为已关闭（TRADE_CLOSED）
     * 3. 恢复商品库存（防止库存被占用）
     * 4. 退还用户优惠券（如果使用了优惠券）
     * <p>
     * 事务边界：@Transactional 保证上述操作原子性，失败则全部回滚
     *
     * @param orderId 订单ID
     * @param reason 取消原因（记录到日志和数据库）
     * @return 操作结果 Map（success/message）
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
            // 状态值："1"-未付款, "2"-已付款, "5"-交易成功, "6"-交易关闭
            if ("6".equals(status) || "5".equals(status)) {
                resultMap.put("success", false);
                resultMap.put("message", "订单状态不允许取消");
                return resultMap;
            }

            // 3. 根据订单状态处理
            if ("2".equals(status)) {
                // 已付款，需要走退款流程
                resultMap.put("success", false);
                resultMap.put("message", "订单已付款，请申请退款");
                return resultMap;
            } else {
                // 未付款，直接取消
                // 3.1 更新订单状态
                order.setStatus("6");
                order.setCloseTime(new Date());
                order.setCancelReason(reason);
                orderMapper.updateByPrimaryKey(order);

                // 3.2 恢复库存（调用私有方法）
                restoreStock(orderId);

                // 3.3 退还优惠券（调用私有方法）
                returnCoupon(orderId);

                resultMap.put("success", true);
                resultMap.put("message", "订单取消成功");
                logger.info("订单取消成功: " + orderId + ", 原因: " + reason);
            }

        } catch (InsufficientStockException e) {
            logger.error("订单取消失败（库存恢复异常）: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "订单取消失败：库存恢复失败");
            throw e;
        } catch (RuntimeException e) {
            logger.error("订单取消失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "订单取消失败：" + e.getMessage());
            throw e;
        }

        return resultMap;
    }

    /**
     * 退款申请功能
     * <p>
     * 业务流程：
     * 1. 验证订单存在性和状态（必须是已付款状态）
     * 2. 检查是否已申请过退款（防止重复退款）
     * 3. 验证退款金额不超过订单金额
     * 4. 创建退款记录，状态设为待审核
     * 5. 更新订单状态为退款申请中
     * <p>
     * 状态流转：
     * - 订单状态：TRADE_SUCCESS -> REFUND_APPLY
     * - 退款记录：创建 -> 待处理(0)
     * <p>
     * 退款审核流程：
     * - 待处理(0) -> 退款成功(1) / 退款失败(2)
     * <p>
     * 注意事项：
     * - 退款申请后需要管理员审核，不能立即退款
     * - 审核通过后调用 confirmRefund() 完成退款
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     * @param refundFee 退款金额（不能大于订单金额）
     * @return 操作结果 Map（success/message）
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
            if (!"2".equals(order.getStatus())) {
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

            // 6. 更新订单状态为已付款（退款处理中，退款状态由退款记录单独跟踪）
            order.setStatus("2");
            order.setCancelReason(reason);
            orderMapper.updateByPrimaryKey(order);

            resultMap.put("success", true);
            resultMap.put("message", "退款申请提交成功，等待审核");
            logger.info("退款申请成功: " + orderId + ", 金额: " + refundFee + ", 原因: " + reason);

        } catch (RuntimeException e) {
            logger.error("退款申请失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "退款申请失败：" + e.getMessage());
            throw e;
        }

        return resultMap;
    }

    /**
     * 确认退款（审核通过）
     * <p>
     * 退款成功后的处理流程：
     * 1. 验证订单和退款记录有效性
     * 2. 调用微信退款API（当前为模拟实现）
     * 3. 更新退款记录状态为成功
     * 4. 更新订单状态为已关闭
     * 5. 恢复商品库存
     * 6. 退还用户优惠券
     * <p>
     * 状态流转：
     * - 退款记录：待处理(0) -> 退款成功(1)
     * - 订单状态：REFUND_APPLY -> TRADE_CLOSED
     * <p>
     * 事务说明：
     * - 如果微信退款API调用失败，整个事务回滚
     * - 库存恢复和优惠券退还失败也会回滚
     * <p>
     * ⚠️ 注意事项：
     * - 当前 weixinRefund() 为模拟实现，需要对接真实微信退款API
     * - 微信退款需要双向证书认证，不是简单的HTTP请求
     * - 退款金额可以小于或等于订单金额（支持部分退款）
     * - 退款成功后，用户余额/银行卡会在1-7天内到账
     *
     * @param orderId 订单ID
     * @return 操作结果 Map（success/message/transactionId）
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

            // 5. 更新订单状态为交易关闭
            order.setStatus("6");
            order.setCloseTime(new Date());
            orderMapper.updateByPrimaryKey(order);

            // 6. 恢复库存
            restoreStock(orderId);

            // 7. 退还优惠券
            returnCoupon(orderId);

            resultMap.put("success", true);
            resultMap.put("message", "退款成功");
            logger.info("退款成功: " + orderId + ", 金额: " + refund.getRefundFee());

        } catch (RuntimeException e) {
            logger.error("退款失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "退款失败：" + e.getMessage());
            throw e;
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
            order.setStatus("2");
            orderMapper.updateByPrimaryKey(order);

            resultMap.put("success", true);
            resultMap.put("message", "已拒绝退款申请");
            logger.info("拒绝退款: " + orderId + ", 原因: " + reason);

        } catch (RuntimeException e) {
            logger.error("拒绝退款失败: " + orderId, e);
            resultMap.put("success", false);
            resultMap.put("message", "操作失败：" + e.getMessage());
            throw e;
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
     * 恢复商品库存
     * <p>
     * 业务逻辑：
     * 1. 查询订单对应的所有订单项
     * 2. 遍历订单项，恢复每个商品的库存
     * <p>
     * SQL执行逻辑：
     * UPDATE tb_item SET stock_count = stock_count + ? WHERE id = ?
     * <p>
     * 调用时机：
     * - 订单取消时（未付款订单）
     * - 退款成功时（已付款订单）
     * <p>
     * ⚠️ 注意事项：
     * - restoreStockCount() 方法定义在 TbItemMapper 中，但实际上应该属于 TbOrderMapper
     * - 该方法违反单一职责原则，TbItemMapper 不应该包含订单相关的库存恢复逻辑
     * - TODO: 建议将 restoreStockCount 移动到 TbOrderMapper，或创建 OrderItemService
     *
     * @param orderId 订单ID
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
        // TODO: 需要在TbOrder中添加user_coupon_id字段后实现
    }

    /**
     * 调用微信退款接口（⚠️ 当前为模拟实现）
     * <p>
     * 🔴 生产环境必须替换为真实的微信退款API调用
     * <p>
     * 微信退款API文档：
     * - 接口地址: https://api.mch.weixin.qq.com/secapi/pay/refund
     * - 请求方式: POST XML
     * - 认证方式: 双向证书认证（商户证书）
     * <p>
     * 必需参数：
     * - appid: 小程序ID
     * - mch_id: 商户号
     * - nonce_str: 随机字符串
     * - sign: 签名
     * - out_trade_no: 原订单号
     * - out_refund_no: 商户退款单号（需要生成新的唯一ID）
     * - total_fee: 订单总金额（分）
     * - refund_fee: 退款金额（分）
     * <p>
     * 实现步骤：
     * 1. 生成商户退款单号（使用 idWorker.nextId()）
     * 2. 组装请求参数
     * 3. 使用商户证书签名
     * 4. 发送HTTPS请求（需配置SSL证书）
     * 5. 解析XML响应
     * 6. 验证签名和返回码
     * <p>
     * 安全注意事项：
     * - 必须使用 HTTPS 协议
     * - 必须验证微信服务器的SSL证书
     * - 签名算法使用 HMAC-SHA256
     * - 密钥不能硬编码，应从配置文件或配置中心读取
     *
     * @param order 订单实体
     * @param refundFee 退款金额（元）
     * @return 退款结果 Map（success/message/transactionId）
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
