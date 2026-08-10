package com.pinyougou.seckill.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.seckill.service.SeckillOrderService;
import com.pinyougou.pojo.TbSeckillOrder;

import entity.Result;

/**
 *  秒杀支付功能
 */
@RestController
@RequestMapping("/pay")
public class PayController {

	private static final Logger logger = Logger.getLogger(PayController.class);

	// 使用固定线程池管理异步支付查询任务，防止裸线程泄漏
	private final ExecutorService payQueryExecutor = Executors.newFixedThreadPool(10,
			new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "seckill-pay-query-" + System.nanoTime());
				}
			});

	@Reference
	private WeixinPayService weixinPayService;

	@Reference
	private SeckillOrderService seckillOrderService;

	@PreDestroy
	public void destroy() {
		payQueryExecutor.shutdown();
	}

	@RequestMapping("/createNative")
	public Map createNative(){
		//1.获取当前登录用户
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		//2.提取秒杀订单（从缓存 ）
		TbSeckillOrder seckillOrder = seckillOrderService.searchOrderFromRedisByUserId(username);
		//3.调用微信支付接口
		if(seckillOrder!=null){
			return weixinPayService.createNative(seckillOrder.getId()+"", (long)(seckillOrder.getMoney().doubleValue()*100)+"");
		}else{
			return new HashMap<>();
		}
	}


	@RequestMapping("/queryPayStatus")
	public DeferredResult<Result> queryPayStatus(final String out_trade_no){
		// 使用DeferredResult实现异步查询，避免阻塞HTTP线程
		final DeferredResult<Result> deferredResult = new DeferredResult<Result>(300000L);

		// 获取当前登录用户
		final String username = SecurityContextHolder.getContext().getAuthentication().getName();

		// 使用线程池替代裸线程，防止线程泄漏和资源耗尽
		payQueryExecutor.execute(new Runnable() {
			@Override
			public void run() {
				int checkCount = 0;
				final int MAX_CHECK_COUNT = 100; // 最多查询100次（300秒）

				while (checkCount < MAX_CHECK_COUNT) {
					try {
						Map<String,String> map = weixinPayService.queryPayStatus(out_trade_no);
						if(map==null){
							deferredResult.setResult(new Result(false, "支付发生错误"));
							return;
						}
						if("SUCCESS".equals(map.get("trade_state"))){
							// 保存订单
							seckillOrderService.saveOrderFromRedisToDb(username, Long.valueOf(out_trade_no), map.get("transaction_id"));
							deferredResult.setResult(new Result(true, "支付成功"));
							return;
						}

						Thread.sleep(3000); // 3秒查询一次
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

				// 超时处理
				Result result = new Result(false, "二维码超时");

				// 关闭支付
				Map<String,String> payResult = weixinPayService.closePay(out_trade_no);
				if(payResult!=null && "FAIL".equals(payResult.get("return_code"))){
					if("ORDERPAID".equals(payResult.get("err_code"))){
						result = new Result(true, "支付成功");
						// 保存订单
						try {
							seckillOrderService.saveOrderFromRedisToDb(username, Long.valueOf(out_trade_no), null);
						} catch (Exception e) {
							logger.error("保存订单失败", e);
						}
					}
				}

				// 删除订单
				if(!result.isSuccess()){
					try {
						seckillOrderService.deleteOrderFromRedis(username, Long.valueOf(out_trade_no));
					} catch (Exception e) {
						logger.error("删除订单失败", e);
					}
				}

				deferredResult.setResult(result);
			}
		});

		return deferredResult;
	}

}
