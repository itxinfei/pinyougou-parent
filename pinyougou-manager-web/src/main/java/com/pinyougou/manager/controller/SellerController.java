package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbSeller;
import com.pinyougou.sellergoods.service.SellerService;
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
import java.util.stream.Collectors;

/**
 * 商家管理
 * <p>
 * 权限要求：管理员
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/seller")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SellerController {

    private static final Logger logger = Logger.getLogger(SellerController.class);

    @Reference
    private SellerService sellerService;

    /**
     * 查询所有商家
     *
     * @return 商家列表
     */
    @GetMapping("/findAll")
    public List<TbSeller> findBySeller() {
        return sellerService.findAll();
    }

    /**
     * 分页查询
     *
     * @param page 当前页码
     * @param rows 每页大小
     * @return 分页结果
     */
    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int rows) {
        return sellerService.findPage(page, rows);
    }

    /**
     * 根据ID查询商家
     *
     * @param id 商家ID
     * @return 商家信息
     */
    @GetMapping("/findOne")
    public TbSeller findOne(@RequestParam(required = true) String id) {
        return sellerService.findOne(id);
    }

    /**
     * 添加商家
     *
     * @param tbSeller 商家信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result add(@Valid @RequestBody TbSeller tbSeller) {
        try {
            sellerService.add(tbSeller);
            logger.info("添加商家成功: " + tbSeller.getSellerId());
            return new Result(true, "添加成功");
        } catch (DataIntegrityViolationException e) {
            logger.error("添加商家失败，商家ID已存在: " + tbSeller.getSellerId(), e);
            return new Result(false, "商家ID已存在，请检查后重试");
        } catch (Exception e) {
            logger.error("添加商家失败: " + tbSeller.getSellerId(), e);
            return new Result(false, "添加失败");
        }
    }

    /**
     * 更新商家信息
     *
     * @param tbSeller 商家信息
     * @return 操作结果
     */
    @RequestMapping(value="/update")
    public Result update(@Valid @RequestBody TbSeller tbSeller) {
        try {
            sellerService.update(tbSeller);
            logger.info("更新商家成功: " + tbSeller.getSellerId());
            return new Result(true, "更新成功");
        } catch (Exception e) {
            logger.error("更新商家失败: " + tbSeller.getSellerId(), e);
            return new Result(false, "更新失败");
        }
    }

    /**
     * 更新商家状态
     *
     * @param sellerId 商家ID
     * @param status   状态值
     * @return 操作结果
     */
    @RequestMapping(value="/updateStatus")
    public Result updateStatus(@RequestParam(required = true) String sellerId,
                               @RequestParam(required = true) String status) {
        try {
            logger.info("更新商家状态: sellerId=" + sellerId + ", status=" + status);
            sellerService.updateStatus(sellerId, status);
            return new Result(true, "状态更新成功");
        } catch (Exception e) {
            logger.error("更新商家状态失败: sellerId=" + sellerId + ", status=" + status, e);
            return new Result(false, "状态更新失败");
        }
    }

    /**
     * 条件查询+分页
     *
     * @param seller 查询条件
     * @param page   当前页码
     * @param rows   每页大小
     * @return 分页结果
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody TbSeller seller,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int rows) {
        return sellerService.findPage(seller, page, rows);
    }
}
