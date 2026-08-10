package com.pinyougou.user.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbAddress;
import com.pinyougou.user.service.AddressService;

import entity.Result;

/**
 * 用户地址控制器
 * 处理用户中心地址管理相关请求
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    private static final Logger logger = Logger.getLogger(AddressController.class);

    @Reference
    private AddressService addressService;

    /**
     * 获取当前用户的地址列表
     */
    @RequestMapping("/findListByLoginUser")
    public List<TbAddress> findListByLoginUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return addressService.findListByUserId(username);
    }

    /**
     * 新增地址
     */
    @RequestMapping("/add")
    public Result add(@RequestBody TbAddress address) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            address.setUserId(username);
            addressService.add(address);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加地址失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改地址
     */
    @RequestMapping("/update")
    public Result update(@RequestBody TbAddress address) {
        try {
            addressService.update(address);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改地址失败", e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 删除地址
     */
    @RequestMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            addressService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除地址失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 设置默认地址
     */
    @RequestMapping("/setDefault")
    public Result setDefault(Long id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            addressService.setDefault(username, id);
            return new Result(true, "设置成功");
        } catch (Exception e) {
            logger.error("设置默认地址失败", e);
            return new Result(false, "设置失败");
        }
    }
}
