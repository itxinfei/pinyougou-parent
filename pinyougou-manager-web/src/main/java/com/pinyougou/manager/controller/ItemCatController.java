package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.sellergoods.service.ItemCatService;
import entity.PageResult;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类管理
 * <p>
 * 权限要求：管理员或运营人员
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/itemCat")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class ItemCatController {

    private static final Logger logger = Logger.getLogger(ItemCatController.class);

    @Reference
    private ItemCatService itemCatService;

    /**
     * 查询所有商品分类
     *
     * @return 分类列表
     */
    @GetMapping("/findAll")
    public List<TbItemCat> findAll() {
        return itemCatService.findAll();
    }

    /**
     * 新增商品分类
     *
     * @param tbItemCat 分类信息
     * @return 操作结果
     */
    @PostMapping("/save")
    public Result save(@Valid @RequestBody TbItemCat tbItemCat) {
        try {
            itemCatService.add(tbItemCat);
            logger.info("保存商品分类成功: " + tbItemCat.getName());
            return new Result(true, "保存成功!");
        } catch (Exception e) {
            logger.error("保存商品分类失败", e);
            return new Result(false, "保存失败!");
        }
    }

    /**
     * 删除商品分类
     *
     * @param ids 分类ID数组
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result(false, "请选择要删除的分类");
        }
        try {
            String idStr = Arrays.stream(ids)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            logger.info("批量删除商品分类: ids=" + idStr);
            itemCatService.delete(ids);
            return new Result(true, "删除成功!");
        } catch (Exception e) {
            logger.error("批量删除商品分类失败", e);
            return new Result(false, "删除失败!");
        }
    }

    /**
     * 分页查询
     *
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize) {
        return itemCatService.findPage(pageNum, pageSize);
    }

    /**
     * 根据上级ID查询子分类
     *
     * @param parentId 上级分类ID
     * @return 子分类列表
     */
    @GetMapping("/findByParentId")
    public List<TbItemCat> findByParentId(@RequestParam(required = true) Long parentId) {
        return itemCatService.findByParentId(parentId);
    }
}
