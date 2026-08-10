package com.pinyougou.manager.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbSpecificationOption;
import com.pinyougou.sellergoods.service.SpecificationOptionService;

import entity.PageResult;
import entity.Result;

/**
 * 规格选项
 * <p>
 * 权限要求：管理员或运营人员
 */
@RestController
@RequestMapping("/specificationOption")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class SpecificationOptionController {

    private static final Logger logger = Logger.getLogger(SpecificationOptionController.class);

    @Reference
    private SpecificationOptionService specificationOptionService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public List<TbSpecificationOption> findAll() {
        return specificationOptionService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return specificationOptionService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param specificationOption
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbSpecificationOption specificationOption) {
        try {
            specificationOptionService.add(specificationOption);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加规格选项失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param specificationOption
     * @return
     */
    @RequestMapping(value="/update")
    public Result update(@RequestBody TbSpecificationOption specificationOption) {
        try {
            specificationOptionService.update(specificationOption);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改规格选项失败", e);
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
    public TbSpecificationOption findOne(@RequestParam(required = true) Long id) {
        return specificationOptionService.findOne(id);
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
            specificationOptionService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除规格选项失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param specificationOption
     * @param page
     * @param rows
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbSpecificationOption specificationOption,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        return specificationOptionService.findPage(specificationOption, page, rows);
    }

}
