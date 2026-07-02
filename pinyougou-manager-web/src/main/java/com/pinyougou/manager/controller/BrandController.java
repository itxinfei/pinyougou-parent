package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbBrand;
import com.pinyougou.sellergoods.service.BrandService;
import entity.PageResult;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 品牌管理
 * <p>
 * 权限要求：管理员或运营人员
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/brand")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class BrandController {

    private static final Logger logger = Logger.getLogger(BrandController.class);

    @Reference
    private BrandService brandService;

    /**
     * 查询所有品牌列表
     */
    @GetMapping("/findAll")
    public List<TbBrand> findAll() {
        return brandService.findAll();
    }

    /**
     * 保存品牌
     *
     * @param brand 品牌信息
     * @return 操作结果
     */
    @PostMapping("/save")
    public Result save(@Valid @RequestBody TbBrand brand) {
        try {
            brandService.save(brand);
            logger.info("保存品牌成功: " + brand.getName());
            return new Result(true, "保存成功!");
        } catch (DataIntegrityViolationException e) {
            logger.error("保存品牌失败，数据已存在: " + brand.getName(), e);
            return new Result(false, "品牌已存在，请检查后重试!");
        } catch (Exception e) {
            logger.error("保存品牌失败: " + brand.getName(), e);
            return new Result(false, "保存失败!");
        }
    }

    /**
     * 批量删除品牌
     *
     * @param ids 品牌ID数组
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result(false, "请选择要删除的品牌");
        }
        try {
            logger.info("批量删除品牌，IDs: " + String.join(",", Arrays.stream(ids).map(String::valueOf).toArray(String[]::new)));
            brandService.delete(ids);
            return new Result(true, "删除成功!");
        } catch (Exception e) {
            logger.error("删除品牌失败，IDs: " + String.join(",", Arrays.stream(ids).map(String::valueOf).toArray(String[]::new)), e);
            return new Result(false, "删除失败!");
        }
    }

    /**
     * 修改品牌
     *
     * @param brand 品牌信息
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result update(@Valid @RequestBody TbBrand brand) {
        try {
            brandService.update(brand);
            logger.info("修改品牌成功: id=" + brand.getId() + ", name=" + brand.getName());
            return new Result(true, "修改成功!");
        } catch (Exception e) {
            logger.error("修改品牌失败: id=" + brand.getId(), e);
            return new Result(false, "修改失败!");
        }
    }

    /**
     * 根据ID查询品牌
     *
     * @param id 品牌ID
     * @return 品牌信息
     */
    @GetMapping("/findById")
    public TbBrand findById(@RequestParam(required = true) Long id) {
        return brandService.findById(id);
    }

    /**
     * 分页搜索品牌
     *
     * @param brand 搜索条件
     * @param page  当前页码
     * @param rows  每页大小
     * @return 分页结果
     */
    @PostMapping("/search")
    public PageResult search(@Valid @RequestBody TbBrand brand,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        return brandService.findByPage(brand, page, rows);
    }

    /**
     * 查询下拉选项列表
     *
     * @return 选项列表
     */
    @GetMapping("/selectOptionList")
    public List<Map> selectOptionList() {
        return brandService.selectOptionList();
    }
}

