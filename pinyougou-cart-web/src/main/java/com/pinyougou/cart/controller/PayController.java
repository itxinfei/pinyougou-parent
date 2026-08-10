package com.pinyougou.cart.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.context.request.async.DeferredResult;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbPayLog;

import entity.Result;
import util.IdWorker;

@RestController
@RequestMapping("/pay")
public class PayController {

	private static final Logger logger = Logger.getLogger(PayController.class);

	// 使用固定线程池管理异步支付查询任务，防止裸线程泄漏
	// 10个线程足以支撑支付轮询的并发量（每个任务最多60秒）
	private final ExecutorService payQueryExecutor = Executors.newFixedThreadPool(10,
			r -> new Thread(r, "pay-query-" + System.nanoTime()));

	@Reference
	private WeixinPayService weixinPayService;

	@Reference
	private OrderService orderService;

	@RequestMapping("/createNative")
	public Map createNative(){
		//1.获取当前登录用户
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		//2.提取支付日志（从缓存 ）
		TbPayLog payLog = orderService.searchPayLogFromRedis(username);
		//3.调用微信支付接口
		if(payLog!=null){
			return weixinPayService.createNative(payLog.getOutTradeNo(), payLog.getTotalFee()+"");
		}else{
			return new HashMap<>();
		}
	}


	@RequestMapping("/queryPayStatus")
	public DeferredResult<Result> queryPayStatus(String out_trade_no){
		// 使用DeferredResult实现异步查询，避免阻塞HTTP线程
		// 超时时间设置为5分钟（微信支付二维码有效时间）
		DeferredResult<Result> deferredResult = new DeferredResult<>(300000L);

		// 使用线程池替代裸线程，防止线程泄漏和资源耗尽
		payQueryExecutor.execute(() -> {
			int checkCount = 0;
			final int MAX_CHECK_COUNT = 60; // 最多查询60次

			while (checkCount < MAX_CHECK_COUNT) {
				try {
					Map<String,String> map = weixinPayService.queryPayStatus(out_trade_no);
					if(map==null){
						deferredResult.setResult(new Result(false, "支付发生错误"));
						return;
					}
					String tradeState = map.get("trade_state");
					if("SUCCESS".equals(tradeState)){
						orderService.updateOrderStatus(out_trade_no, map.get("transaction_id"));
						deferredResult.setResult(new Result(true, "支付成功"));
						return;
					}

					Thread.sleep(1000); // 1秒查询一次
					checkCount++;
				} catch (InterruptedException e) {
					logger.error("支付状态查询中断", e);
					deferredResult.setResult(new Result(false, "支付查询被中断"));
					return;
				} catch (Exception e) {
					logger.error("支付状态查询异常", e);
					deferredResult.setResult(new Result(false, "支付查询异常"));
					return;
				}
			}

			deferredResult.setResult(new Result(false, "二维码超时"));
		});

		return deferredResult;
	}


}
