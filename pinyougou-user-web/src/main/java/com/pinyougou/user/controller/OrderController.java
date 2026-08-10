package com.pinyougou.user.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.order.service.OrderService;

import entity.PageResult;
import entity.Result;

/**
 * 用户订单控制器
 * 处理用户中心订单相关请求
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = Logger.getLogger(OrderController.class);

    @Reference
    private OrderService orderService;

    /**
     * 搜索订单（分页）
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbOrder order, int page, int rows) {
        // 只查询当前用户的订单
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setUserId(username);
        return orderService.findPage(order, page, rows);
    }

    /**
     * 查询订单详情
     */
    @RequestMapping("/findOne")
    public TbOrder findOne(Long id) {
        return orderService.findOne(id);
    }

    /**
     * 更新订单
     */
    @RequestMapping("/update")
    public Result update(@RequestBody TbOrder order) {
        try {
            orderService.update(order);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改订单失败", e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 删除订单
     */
    @RequestMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            orderService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除订单失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 更新订单状态（取消订单、确认收货等）
     */
    @RequestMapping("/updateStatus")
    public Result updateStatus(Long orderId, String status) {
        try {
            orderService.updateStatus(orderId, status);
            return new Result(true, "操作成功");
        } catch (Exception e) {
            logger.error("更新订单状态失败", e);
            return new Result(false, e.getMessage() != null ? e.getMessage() : "操作失败");
        }
    }
}
