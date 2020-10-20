package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbSeller;
import com.pinyougou.sellergoods.service.SellerService;
import entity.PageResult;
import entity.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家管理
 */
@RestController
@RequestMapping("/seller")
public class SellerController {

    @Reference
    private SellerService sellerService;

    /**
     * 查询所有商家管理
     *
     * @return
     */
    @RequestMapping("/findAll")
    public List<TbSeller> findBySeller() {
        return sellerService.findAll();
    }

    /**
     * 分页
     *
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return sellerService.findPage(page, rows);
    }

    /**
     * 根据ID获取实体,返回一个实体
     *
     * @param id
     * @return
     */
    @RequestMapping("/findOne")
    public TbSeller findOne(String id) {
        return sellerService.findOne(id);
    }


    /**
     * 添加商家管理
     */
    @RequestMapping("/add")
    public void add(TbSeller tbSeller) {
        sellerService.add(tbSeller);

    }

    /**
     * 更改商家管理
     */
    @RequestMapping("/update")
    public void update(TbSeller tbSeller) {
        sellerService.update(tbSeller);
    }

    /**
     * 更新商家管理状态
     */
    @RequestMapping("/updateStatus")
    public Result updateStatus(String sellerId, String status) {
        try {
            sellerService.updateStatus(sellerId, status);
            return new Result(true, "成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result(false, "更新失败");
    }

    /**
     * 查询+分页
     *
     * @param seller
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbSeller seller, int page, int rows) {
        return sellerService.findPage(seller, page, rows);
    }

}
