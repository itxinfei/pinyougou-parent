package com.pinyougou.manager.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.content.service.ContentCategoryService;
import com.pinyougou.pojo.TbContentCategory;

import entity.PageResult;
import entity.Result;

/**
 * 内容（广告）类型
 */
@RestController
@RequestMapping("/contentCategory")
public class ContentCategoryController {

    private static final Logger logger = Logger.getLogger(ContentCategoryController.class);

    @Reference
    private ContentCategoryService contentCategoryService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public List<TbContentCategory> findAll() {
        return contentCategoryService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return contentCategoryService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param contentCategory
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbContentCategory contentCategory) {
        try {
            contentCategoryService.add(contentCategory);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加内容分类失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param contentCategory
     * @return
     */
    @PutMapping("/update")
    public Result update(@RequestBody TbContentCategory contentCategory) {
        try {
            contentCategoryService.update(contentCategory);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改内容分类失败", e);
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
    public TbContentCategory findOne(Long id) {
        return contentCategoryService.findOne(id);
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
            contentCategoryService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除内容分类失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param brand
     * @param page
     * @param rows
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbContentCategory contentCategory, int page, int rows) {
        return contentCategoryService.findPage(contentCategory, page, rows);
    }

}
