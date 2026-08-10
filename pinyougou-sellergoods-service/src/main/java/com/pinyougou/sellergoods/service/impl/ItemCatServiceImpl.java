package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.GenericMapper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.pojo.TbItemCatExample;
import com.pinyougou.sellergoods.service.ItemCatService;
import com.pinyougou.service.BaseServiceImpl;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 商品分类管理
 */
@Service
public class ItemCatServiceImpl extends BaseServiceImpl<TbItemCat> implements ItemCatService {

    @Autowired
    private TbItemCatMapper tbItemCatMapper;

    @Override
    protected GenericMapper<TbItemCat> getMapper() {
        return tbItemCatMapper;
    }

    /**
     * 条件查询带分页
     */
    @Override
    public PageResult findPage(TbItemCat itemCat, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        TbItemCatExample example = new TbItemCatExample();
        TbItemCatExample.Criteria criteria = example.createCriteria();
        if (itemCat != null) {
            if (itemCat.getName() != null && itemCat.getName().length() > 0) {
                criteria.andNameLike("%" + itemCat.getName() + "%");
            }
        }
        Page<TbItemCat> page = (Page<TbItemCat>) tbItemCatMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据上级ID返回列表
     */
    @Override
    public List<TbItemCat> findByParentId(Long parentId) {
        TbItemCatExample example1 = new TbItemCatExample();
        TbItemCatExample.Criteria criteria1 = example1.createCriteria();
        criteria1.andParentIdEqualTo(parentId);
        return tbItemCatMapper.selectByExample(example1);
    }
}
