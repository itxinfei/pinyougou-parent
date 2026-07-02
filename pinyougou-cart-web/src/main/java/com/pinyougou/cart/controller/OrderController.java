package com.pinyougou.cart.controller;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.order.service.RefundService;

import entity.PageResult;
import entity.Result;
/**
 * controller
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/order")
public class OrderController {

	private static final Logger logger = Logger.getLogger(OrderController.class);

	@Reference
	private OrderService orderService;

	@Reference
	private RefundService refundService;
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findAll")
	public List<TbOrder> findAll(){			
		return orderService.findAll();
	}
	
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findPage")
	public PageResult  findPage(int page,int rows){			
		return orderService.findPage(page, rows);
	}
	
	/**
	 * 增加
	 * @param order
	 * @return
	 */
	@RequestMapping("/add")
	public Result add(@RequestBody TbOrder order){
		
		//获取当前登录人账号
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		order.setUserId(username);
		order.setSourceType("2");//订单来源  PC
		
		try {
			orderService.add(order);
			return new Result(true, "增加成功");
		} catch (Exception e) {
			logger.error("增加订单失败", e);
			return new Result(false, "增加失败");
		}
	}
	
	/**
	 * 修改
	 * @param order
	 * @return
	 */
	@RequestMapping("/update")
	public Result update(@RequestBody TbOrder order){
		try {
			orderService.update(order);
			return new Result(true, "修改成功");
		} catch (Exception e) {
			logger.error("修改订单失败", e);
			return new Result(false, "修改失败");
		}
	}	
	
	/**
	 * 获取实体
	 * @param id
	 * @return
	 */
	@RequestMapping("/findOne")
	public TbOrder findOne(Long id){
		TbOrder order = orderService.findOne(id);
		// 校验订单所有权，防止IDOR
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		if (order != null && !username.equals(order.getUserId()) && !"anonymousUser".equals(username)) {
			logger.warn("用户 " + username + " 尝试访问不属于自己的订单: " + id);
			return null;
		}
		return order;
	}
	
	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@RequestMapping("/delete")
	public Result delete(Long [] ids){
		try {
			orderService.delete(ids);
			return new Result(true, "删除成功"); 
		} catch (Exception e) {
			logger.error("删除订单失败", e);
			return new Result(false, "删除失败");
		}
	}
	
		/**
	 * 查询+分页
	 * @param brand
	 * @param page
	 * @param rows
	 * @return
	 */
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbOrder order, int page, int rows  ){
		return orderService.findPage(order, page, rows);
	}

	/**
	 * 取消订单
	 * @param orderId 订单ID
	 * @param reason 取消原因
	 * @return 取消结果
	 */
	@RequestMapping("/cancel")
	public Result cancel(Long orderId, String reason) {
		// 校验订单所有权
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		TbOrder order = orderService.findOne(orderId);
		if (order == null) {
			return new Result(false, "订单不存在");
		}
		if (!username.equals(order.getUserId())) {
			logger.warn("用户 " + username + " 尝试取消不属于自己的订单: " + orderId);
			return new Result(false, "无权操作此订单");
		}
		try {
			Map<String, Object> result = refundService.cancelOrder(orderId, reason);
			boolean success = (boolean) result.get("success");
			String message = (String) result.get("message");
			return new Result(success, message);
		} catch (Exception e) {
			logger.error("取消订单失败", e);
			return new Result(false, "取消订单失败");
		}
	}

	/**
	 * 申请退款
	 * @param orderId 订单ID
	 * @param reason 退款原因
	 * @param refundFee 退款金额
	 * @return 退款结果
	 */
	@RequestMapping("/applyRefund")
	public Result applyRefund(Long orderId, String reason, Double refundFee) {
		// 校验订单所有权
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		TbOrder order = orderService.findOne(orderId);
		if (order == null) {
			return new Result(false, "订单不存在");
		}
		if (!username.equals(order.getUserId())) {
			logger.warn("用户 " + username + " 尝试退款不属于自己的订单: " + orderId);
			return new Result(false, "无权操作此订单");
		}
		try {
			Map<String, Object> result = refundService.applyRefund(orderId, reason, refundFee);
			boolean success = (boolean) result.get("success");
			String message = (String) result.get("message");
			return new Result(success, message);
		} catch (Exception e) {
			logger.error("申请退款失败", e);
			return new Result(false, "申请退款失败");
		}
	}

	/**
	 * 确认退款
	 * @param orderId 订单ID
	 * @return 退款结果
	 */
	@RequestMapping("/confirmRefund")
	public Result confirmRefund(Long orderId) {
		// 校验订单所有权
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		TbOrder order = orderService.findOne(orderId);
		if (order == null) {
			return new Result(false, "订单不存在");
		}
		if (!username.equals(order.getUserId())) {
			logger.warn("用户 " + username + " 尝试确认退款不属于自己的订单: " + orderId);
			return new Result(false, "无权操作此订单");
		}
		try {
			Map<String, Object> result = refundService.confirmRefund(orderId);
			boolean success = (boolean) result.get("success");
			String message = (String) result.get("message");
			return new Result(success, message);
		} catch (Exception e) {
			logger.error("确认退款失败", e);
			return new Result(false, "确认退款失败");
		}
	}

}
