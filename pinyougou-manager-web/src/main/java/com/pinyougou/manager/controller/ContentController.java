package com.pinyougou.manager.controller;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.content.service.ContentService;
import com.pinyougou.pojo.TbContent;

import entity.PageResult;
import entity.Result;

/**
 * 内容（广告）
 * <p>
 * 权限要求：管理员或运营人员
 */
@RestController
@RequestMapping("/content")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class ContentController {

    private static final Logger logger = Logger.getLogger(ContentController.class);

    @Reference
    private ContentService contentService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public List<TbContent> findAll() {
        return contentService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return contentService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param content
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbContent content) {
        try {
            contentService.add(content);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("增加内容失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param content
     * @return
     */
    @PutMapping("/update")
    public Result update(@RequestBody TbContent content) {
        try {
            contentService.update(content);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改内容失败", e);
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
    public TbContent findOne(@RequestParam(required = true) Long id) {
        return contentService.findOne(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        try {
            contentService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("删除内容失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param content
     * @param page
     * @param rows
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbContent content,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        return contentService.findPage(content, page, rows);
    }

}
