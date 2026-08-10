package com.pinyougou.content.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.content.service.ContentCategoryService;
import com.pinyougou.mapper.GenericMapper;
import com.pinyougou.mapper.TbContentCategoryMapper;
import com.pinyougou.pojo.TbContentCategory;
import com.pinyougou.pojo.TbContentCategoryExample;
import com.pinyougou.pojo.TbContentCategoryExample.Criteria;
import com.pinyougou.service.BaseServiceImpl;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class ContentCategoryServiceImpl extends BaseServiceImpl<TbContentCategory> implements ContentCategoryService {

    @Autowired
    private TbContentCategoryMapper contentCategoryMapper;

    @Override
    protected GenericMapper<TbContentCategory> getMapper() {
        return contentCategoryMapper;
    }

    @Override
    public PageResult findPage(TbContentCategory contentCategory, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbContentCategoryExample example = new TbContentCategoryExample();
        Criteria criteria = example.createCriteria();

        if (contentCategory != null) {
            if (contentCategory.getName() != null && contentCategory.getName().length() > 0) {
                criteria.andNameLike("%" + contentCategory.getName() + "%");
            }

        }

        Page<TbContentCategory> page = (Page<TbContentCategory>) contentCategoryMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }
}
