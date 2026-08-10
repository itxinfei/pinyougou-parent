package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbSpecification;
import com.pinyougou.pojo.group.Specification;
import com.pinyougou.sellergoods.service.SpecificationService;
import entity.PageResult;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规格管理
 * <p>
 * 权限要求：管理员或运营人员
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/specification")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_OPERATOR')")
public class SpecificationController {

    private static final Logger logger = Logger.getLogger(SpecificationController.class);

    @Reference
    private SpecificationService specificationService;

    /**
     * 查询所有规格
     *
     * @return 规格列表
     */
    @GetMapping("/findAll")
    public List<TbSpecification> findAll() {
        return specificationService.findAll();
    }

    /**
     * 分页查询规格
     *
     * @param page 当前页码
     * @param rows 每页大小
     * @return 分页结果
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return specificationService.findPage(page, rows);
    }

    /**
     * 添加规格
     *
     * @param specification 规格信息（包含规格选项）
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result add(@Valid @RequestBody Specification specification) {
        try {
            specificationService.add(specification);
            logger.info("添加规格成功: " + specification.getSpecification().getSpecName());
            return new Result(true, "增加成功");
        } catch (Exception e) {
            logger.error("添加规格失败", e);
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改规格
     *
     * @param specification 规格信息
     * @return 操作结果
     */
    @RequestMapping(value="/update")
    public Result update(@Valid @RequestBody Specification specification) {
        try {
            specificationService.update(specification);
            logger.info("修改规格成功: id=" + specification.getSpecification().getId());
            return new Result(true, "修改成功");
        } catch (Exception e) {
            logger.error("修改规格失败: id=" + specification.getSpecification().getId(), e);
            return new Result(false, "修改失败");
        }
    }

    /**
     * 根据ID查询规格
     *
     * @param id 规格ID
     * @return 规格信息
     */
    @GetMapping("/findOne")
    public Specification findOne(@RequestParam(required = true) Long id) {
        return specificationService.findOne(id);
    }

    /**
     * 批量删除规格
     *
     * @param ids 规格ID数组
     * @return 操作结果
     */
    @RequestMapping(value="/delete")
    public Result delete(@RequestParam(required = true) Long[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result(false, "请选择要删除的规格");
        }
        try {
            String idStr = Arrays.stream(ids)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            logger.info("批量删除规格: ids=" + idStr);
            specificationService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            logger.error("批量删除规格失败", e);
            return new Result(false, "删除失败");
        }
    }

    /**
     * 条件查询+分页
     *
     * @param specification 查询条件
     * @param page 当前页码
     * @param rows 每页大小
     * @return 分页结果
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody(required = false) TbSpecification specification,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int rows) {
        return specificationService.findPage(specification, page, rows);
    }

    /**
     * 查询下拉选项列表
     *
     * @return 选项列表
     */
    @GetMapping("/selectOptionList")
    public List<Map> selectOptionList() {
        return specificationService.selectOptionList();
    }
}
