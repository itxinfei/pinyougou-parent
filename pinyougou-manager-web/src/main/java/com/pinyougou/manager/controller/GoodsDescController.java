package com.pinyougou.manager.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbGoodsDesc;
import com.pinyougou.sellergoods.service.GoodsDescService;

import entity.PageResult;
import entity.Result;

/**
 * 商品详情
 */
@RestController
@RequestMapping("/goodsDesc")
public class GoodsDescController {

    private static final Logger logger = Logger.getLogger(GoodsDescController.class);

    @Reference
    private GoodsDescService goodsDescService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public List<TbGoodsDesc> findAll() {
        return goodsDescService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return goodsDescService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param goodsDesc
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbGoodsDesc goodsDesc) {
        try {
            goodsDescService.add(goodsDesc);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加商品详情失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param goodsDesc
     * @return
     */
    @PutMapping("/update")
    public Result update(@RequestBody TbGoodsDesc goodsDesc) {
        try {
            goodsDescService.update(goodsDesc);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改商品详情失败", e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 获取实体
     *
     * @param id
     * @return
     */
    @GetMapping("/findOne")
    public TbGoodsDesc findOne(Long id) {
        return goodsDescService.findOne(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            goodsDescService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除商品详情失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param goodsDesc
     * @param page
     * @param rows
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbGoodsDesc goodsDesc, int page, int rows) {
        return goodsDescService.findPage(goodsDesc, page, rows);
    }

}
