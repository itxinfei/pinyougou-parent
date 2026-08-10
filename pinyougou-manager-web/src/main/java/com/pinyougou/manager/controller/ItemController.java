package com.pinyougou.manager.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.sellergoods.service.ItemService;

import entity.PageResult;
import entity.Result;

/**
 * 商品明细
 * <p>
 * 权限要求：管理员或运营人员
 */
@RestController
@RequestMapping("/item")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class ItemController {

    private static final Logger logger = Logger.getLogger(ItemController.class);

    @Reference
    private ItemService itemService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public List<TbItem> findAll() {
        return itemService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return itemService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param item
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbItem item) {
        try {
            itemService.add(item);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加商品明细失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param item
     * @return
     */
    @RequestMapping(value="/update")
    public Result update(@RequestBody TbItem item) {
        try {
            itemService.update(item);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改商品明细失败", e);
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
    public TbItem findOne(@RequestParam(required = true) Long id) {
        return itemService.findOne(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @RequestMapping(value="/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        try {
            itemService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除商品明细失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param item
     * @param page
     * @param rows
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbItem item,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        return itemService.findPage(item, page, rows);
    }

}
