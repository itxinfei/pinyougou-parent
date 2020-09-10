package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbBrandMapper;
import com.pinyougou.pojo.TbBrand;
import com.pinyougou.pojo.TbBrandExample;
import com.pinyougou.pojo.TbBrandExample.Criteria;
import com.pinyougou.sellergoods.service.BrandService;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 品牌管理功能
 */
@Service
@Transactional
public class BrandServiceImpl implements BrandService {

    @Autowired
    private TbBrandMapper brandMapper;

    /**
     * 查询所有
     *
     * @return
     */

    @Override
    public List<TbBrand> findAll() {
        return brandMapper.selectByExample(null);
    }


    /**
     * 保存品牌
     */
    @Override
    public void save(TbBrand brand) {
        brandMapper.insert(brand);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @Override
    public TbBrand findById(Long id) {
        return brandMapper.selectByPrimaryKey(id);
    }

    /**
     * 修改品牌
     *
     * @param brand
     */
    @Override
    public void update(TbBrand brand) {
        brandMapper.updateByPrimaryKey(brand);
    }

    /**
     * 删除多条记录
     *
     * @param ids
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            brandMapper.deleteByPrimaryKey(id);
        }
    }


    // 条件查询带分页
    @Override
    public PageResult findByPage(TbBrand brand, int pageNum, int pageSize) {
        // 使用分页插件:
        PageHelper.startPage(pageNum, pageSize);
        // 进行条件查询:
        TbBrandExample example = new TbBrandExample();
        Criteria criteria = example.createCriteria();
        // 设置条件:
        if (brand.getName() != null && brand.getName().length() > 0) {
            criteria.andNameLike("%" + brand.getName() + "%");
        }
        if (brand.getFirstChar() != null && brand.getFirstChar().length() > 0) {
            criteria.andFirstCharEqualTo(brand.getFirstChar());
        }
        Page<TbBrand> page = (Page<TbBrand>) brandMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * @return
     */
    @Override
    public List<Map> selectOptionList() {
        List<Map> maps = brandMapper.selectOptionList();
        for (Map map : maps) {
            System.out.println("selectOptionList" + map);
        }
        return maps;
    }

}
