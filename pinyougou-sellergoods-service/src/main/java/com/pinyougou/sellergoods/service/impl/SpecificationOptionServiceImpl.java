package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.GenericMapper;
import com.pinyougou.mapper.TbSpecificationOptionMapper;
import com.pinyougou.pojo.TbSpecificationOption;
import com.pinyougou.pojo.TbSpecificationOptionExample;
import com.pinyougou.pojo.TbSpecificationOptionExample.Criteria;
import com.pinyougou.sellergoods.service.SpecificationOptionService;
import com.pinyougou.service.BaseServiceImpl;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class SpecificationOptionServiceImpl extends BaseServiceImpl<TbSpecificationOption> implements SpecificationOptionService {

    @Autowired
    private TbSpecificationOptionMapper specificationOptionMapper;

    @Override
    protected GenericMapper<TbSpecificationOption> getMapper() {
        return specificationOptionMapper;
    }

    @Override
    public PageResult findPage(TbSpecificationOption specificationOption, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbSpecificationOptionExample example = new TbSpecificationOptionExample();
        Criteria criteria = example.createCriteria();

        if (specificationOption != null) {
            if (specificationOption.getOptionName() != null && specificationOption.getOptionName().length() > 0) {
                criteria.andOptionNameLike("%" + specificationOption.getOptionName() + "%");
            }
        }

        Page<TbSpecificationOption> page = (Page<TbSpecificationOption>) specificationOptionMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

}
