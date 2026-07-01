package com.pinyougou.order.service;

import java.util.List;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbPayLog;

import entity.PageResult;

/**
 * 订单服务接口
 * <p>
 * 服务职责：
 * - 订单生命周期管理（创建、查询、取消）
 * - 支付状态同步（支付成功回调）
 * - 支付日志管理
 * <p>
 * 订单状态流转：
 * 1. 未付款(1) -> 已付款(2) [支付成功]
 * 2. 未付款(1) -> 已关闭(TRADE_CLOSED) [用户取消]
 * 3. 已付款(2) -> 已发货(3) [商家发货]
 * 4. 已付款(2) -> 退款申请(REFUND_APPLY) [申请退款]
 * 5. 退款申请(REFUND_APPLY) -> 已关闭(TRADE_CLOSED) [退款成功]
 * 6. 退款申请(REFUND_APPLY) -> 已付款(TRADE_SUCCESS) [退款拒绝]
 * <p>
 * 核心业务场景：
 * - 购物车结算：add() 从购物车创建订单
 * - 微信支付回调：updateOrderStatus() 更新订单状态
 * - 订单取消：cancelOrder() 取消未付款订单
 * - 退款申请：applyRefund() 提交退款申请
 * - 退款审核：confirmRefund()/rejectRefund() 审核退款
 * <p>
 * 技术特性：
 * - 分布式事务：订单、库存、支付日志的原子性操作
 * - 乐观锁：库存扣减使用 WHERE stock_count >= num 防止超卖
 * - 消息队列：支付成功后发送短信通知（通过ActiveMQ）
 * - 缓存同步：订单数据同步到Redis（用于前端展示）
 * <p>
 * 数据库表关联：
 * - TbOrder: 订单主表
 * - TbOrderItem: 订单项表（一个订单对应多个商品）
 * - TbPayLog: 支付日志表（记录支付流水）
 * - TbRefund: 退款记录表（记录退款申请）
 * - TbItem: 商品表（库存扣减/恢复）
 * <p>
 * @author Administrator
 *
 */
public interface OrderService {

	/**
	 * 返回全部列表
	 * @return
	 */
	public List<TbOrder> findAll();


	/**
	 * 返回分页列表
	 * @return
	 */
	public PageResult findPage(int pageNum,int pageSize);


	/**
	 * 增加
	*/
	public void add(TbOrder order);


	/**
	 * 修改
	 */
	public void update(TbOrder order);


	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	public TbOrder findOne(Long id);


	/**
	 * 批量删除
	 * @param ids
	 */
	public void delete(Long [] ids);

	/**
	 * 分页
	 * @param pageNum 当前页 码
	 * @param pageSize 每页记录数
	 * @return
	 */
	public PageResult findPage(TbOrder order, int pageNum,int pageSize);

	/**
	 * 根据用户ID获取支付日志
	 * @param userId
	 * @return
	 */
	public TbPayLog searchPayLogFromRedis(String userId);


	/**
	 * 支付成功修改状态
	 * @param out_trade_no
	 * @param transaction_id
	 */
	public void updateOrderStatus(String out_trade_no,String transaction_id);


}
