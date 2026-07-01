package com.pinyougou.shop.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbTypeTemplate;
import com.pinyougou.sellergoods.service.TypeTemplateService;
import entity.PageResult;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模板管理
 */

@RestController
@RequestMapping("/typeTemplate")
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
    public PageResult findPage(int page, int rows) {
        return typeTemplateService.findPage(page, rows);
    }

    /**
     * 增加
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
     * 修改
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
     * 获取实体
     *
     * @param id
     * @return
     */
    @GetMapping("/findOne")
    public TbTypeTemplate findOne(Long id) {
        return typeTemplateService.findOne(id);
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
            typeTemplateService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除模板失败", e);
            return new Result(false, "删除失败");
        }
    }

    //查询+分页
    @PostMapping("/search")
    public PageResult search(@RequestBody TbTypeTemplate typeTemplate, int page, int rows) {
        return typeTemplateService.findPage(typeTemplate, page, rows);
    }

}
