package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbTypeTemplate;
import com.pinyougou.sellergoods.service.TypeTemplateService;
import entity.PageResult;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板管理
 * 类型模板：用于关联品牌和规格
 * <p>
 * 权限要求：管理员或运营人员
 */
@RestController
@RequestMapping("/typeTemplate")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class TypeTemplateController {

    private static final Logger logger = Logger.getLogger(TypeTemplateController.class);

    @Reference
    private TypeTemplateService typeTemplateService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public List<TbTypeTemplate> findAll() {
        return typeTemplateService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return typeTemplateService.findPage(page, rows);
    }

    /**
     * 增加模板管理
     *
     * @param typeTemplate
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbTypeTemplate typeTemplate) {
        try {
            typeTemplateService.add(typeTemplate);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加模板失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改模板管理
     *
     * @param typeTemplate
     * @return
     */
    @PutMapping("/update")
    public Result update(@RequestBody TbTypeTemplate typeTemplate) {
        try {
            typeTemplateService.update(typeTemplate);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改模板失败", e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 获取模板管理实体
     *
     * @param id
     * @return
     */
    @GetMapping("/findOne")
    public TbTypeTemplate findOne(@RequestParam(required = true) Long id) {
        return typeTemplateService.findOne(id);
    }

    /**
     * 批量删除模板管理
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        try {
            typeTemplateService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除模板失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 模板管理 查询+分页
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbTypeTemplate typeTemplate,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        return typeTemplateService.findPage(typeTemplate, page, rows);
    }

}
