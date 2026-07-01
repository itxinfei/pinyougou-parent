package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.group.Goods;
import com.pinyougou.sellergoods.service.GoodsService;
import entity.PageResult;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品管理
 * <p>
 * 权限要求：管理员或商家（ROLE_ADMIN、ROLE_SELLER）
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/goods")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SELLER')")
public class GoodsController {

    private static final Logger logger = Logger.getLogger(GoodsController.class);

    @Reference
    private GoodsService goodsService;

    /**
     * 查询所有商品
     *
     * @return 商品列表
     */
    @GetMapping("/findAll")
    public List<TbGoods> findAll() {
        return goodsService.findAll();
    }

    /**
     * 分页查询商品
     *
     * @param page 当前页码
     * @param rows 每页大小
     * @return 分页结果
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return goodsService.findPage(page, rows);
    }

    /**
     * 修改商品
     *
     * @param goods 商品信息
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Goods goods) {
        try {
            goodsService.update(goods);
            logger.info("修改商品成功: goodsId=" + goods.getGoods().getId());
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改商品失败: goodsId=" + goods.getGoods().getId(), e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 根据ID查询商品
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/findOne")
    public TbGoods findOne(@RequestParam(required = true) Long id) {
        return goodsService.findById(id);
    }

    /**
     * 批量删除商品
     *
     * @param ids 商品ID数组
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result(false, "请选择要删除的商品");
        }
        try {
            String idStr = Arrays.stream(ids)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            logger.info("批量删除商品，IDs: " + idStr);
            goodsService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("批量删除商品失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 条件查询+分页（商家只能查询自己的商品）
     *
     * @param goods 查询条件
     * @param page  当前页码
     * @param rows  每页大小
     * @return 分页结果
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbGoods goods,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        // 获取当前登录用户（管理员或商家）
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("查询商品: userId=" + userId);
        // 商家只能查询自己的商品
        goods.setSellerId(userId);
        return goodsService.findPage(goods, page, rows);
    }

    /**
     * 批量修改商品状态
     *
     * @param ids   商品ID数组
     * @param status 状态值
     * @return 操作结果
     */
    @PostMapping("/updateStatus")
    public Result updateStatus(@RequestParam(required = true) Long[] ids,
                               @RequestParam(required = true) String status) {
        if (ids == null || ids.length == 0) {
            return new Result(false, "请选择要操作的商品");
        }
        try {
            String idStr = Arrays.stream(ids)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            logger.info("批量修改商品状态: ids=" + idStr + ", status=" + status);
            goodsService.updateStatus(ids, status);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("批量修改商品状态失败", e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 批量删除商品（逻辑删除）
     *
     * @param ids 商品ID数组
     * @return 操作结果
     */
    @DeleteMapping("/updateIsDelete")
    public Result updateIsDelete(@RequestParam(required = true) Long[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result(false, "请选择要删除的商品");
        }
        try {
            String idStr = Arrays.stream(ids)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            logger.info("批量逻辑删除商品: ids=" + idStr);
            goodsService.delete(ids);
            return new Result(true, "操作成功");
        } catch (Exception e) {
            logger.error("批量逻辑删除商品失败", e);
            return new Result(false, "操作失败");
        }
    }

    /**
     * 生成商品详情页HTML
     *
     * @param goodsId 商品ID
     */
    @GetMapping("/genHtml")
    public void genHtml(@RequestParam(required = true) Long goodsId) {
        try {
            logger.info("生成商品详情页: goodsId=" + goodsId);
            // TODO: 实现商品详情页静态化
            // itemPageService.genItemHtml(goodsId);
        } catch (Exception e) {
            logger.error("生成商品详情页失败: goodsId=" + goodsId, e);
        }
    }
}
