package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.pojo.TbItemCatExample;
import com.pinyougou.sellergoods.service.ItemCatService;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类管理
 */
@Service
@Transactional
public class ItemCatServiceImpl implements ItemCatService {

    @Autowired
    private TbItemCatMapper tbItemCatMapper;


    /**
     * 查询所有商品分类管理
     *
     * @return
     */
    @Override
    public List<TbItemCat> findAll() {
        return tbItemCatMapper.selectByExample(null);
    }

    /**
     * 分页查询品牌的方法
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbItemCat> page = (Page<TbItemCat>) tbItemCatMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 添加品牌
     *
     * @param itemCat
     */
    @Override
    public void add(TbItemCat itemCat) {
        tbItemCatMapper.insert(itemCat);
    }

    /**
     * 更改品牌
     *
     * @param itemCat
     */
    @Override
    public void update(TbItemCat itemCat) {
        tbItemCatMapper.updateByPrimaryKey(itemCat);
    }

    /**
     * 根据ID查询实体
     *
     * @param id
     * @return
     */
    @Override
    public TbItemCat findOne(Long id) {
        return tbItemCatMapper.selectByPrimaryKey(id);
    }

    /**
     * 删除品牌
     *
     * @param ids
     */
    public void delete(Long[] ids) {
        tbItemCatMapper.deleteByPrimaryKey(ids);
    }

    /**
     * 条件查询带分页
     */
    @Override
    public PageResult findPage(TbItemCat itemCat, int pageNum, int pageSize) {
        // 使用分页插件:
        PageHelper.startPage(pageNum, pageSize);
        // 进行条件查询:
        TbItemCatExample example = new TbItemCatExample();
        TbItemCatExample.Criteria criteria = example.createCriteria();
        // 设置条件:
        if (itemCat.getName() != null && itemCat.getName().length() > 0) {
            criteria.andNameLike("%" + itemCat.getName() + "%");
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
