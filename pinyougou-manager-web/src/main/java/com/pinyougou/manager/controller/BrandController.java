package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbBrand;
import com.pinyougou.sellergoods.service.BrandService;
import entity.PageResult;
import entity.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 品牌管理
 */
@RestController
@RequestMapping("/brand")
public class BrandController {

    @Reference
    private BrandService brandService;

    /**
     * 查询所有品牌列表
     */
    @RequestMapping("/findAll")
    public List<TbBrand> findAll() {
        return brandService.findAll();
    }

    /**
     * 保存品牌
     */
    @RequestMapping("/save")
    public Result save(@RequestBody TbBrand brand) {
        try {
            brandService.save(brand);
            return new Result(true, "保存成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "保存失败!");
        }
    }

    /**
     * 删除多个品牌
     */
    @RequestMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            brandService.delete(ids);
            return new Result(true, "删除成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败!");
        }
    }

    /**
     * 修改品牌
     */
    @RequestMapping("/update")
    public Result update(@RequestBody TbBrand brand) {
        try {
            brandService.update(brand);
            return new Result(true, "修改成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改失败!");
        }
    }

    /**
     * 根据ID查询品牌
     *
     * @param id
     * @return
     */
    @RequestMapping("/findById")
    public TbBrand findById(Long id) {
        TbBrand byId = brandService.findById(id);
        //System.out.println("后端接收到的ID:" + byId);
        return brandService.findById(id);
    }

    /**
     * 搜索功能
     *
     * @RequestBody，参数必须是post方式提交的，不能是url查询。
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbBrand brand, int page, int rows) {
        return brandService.findByPage(brand, page, rows);
    }

    /**
     * @return
     */
    @RequestMapping("/selectOptionList")
    public List<Map> selectOptionList() {
        List<Map> maps = brandService.selectOptionList();
        return maps;
    }
}

