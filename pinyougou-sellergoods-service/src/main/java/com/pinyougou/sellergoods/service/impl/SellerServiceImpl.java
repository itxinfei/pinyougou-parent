package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbSellerMapper;
import com.pinyougou.pojo.TbSeller;
import com.pinyougou.pojo.TbSellerExample;
import com.pinyougou.sellergoods.service.SellerService;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商家审核
 */
@Service
@Transactional
public class SellerServiceImpl implements SellerService {

    @Autowired
    private TbSellerMapper tbSellerMapper;

    /**
     * 查询所有
     *
     * @return
     */
    @Override
    public List<TbSeller> findAll() {
        return tbSellerMapper.selectByExample(null);
    }

    /**
     * 返回分页列表
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        // 分页插件
        PageHelper.startPage(pageNum, pageSize);
        Page<TbSeller> page = (Page<TbSeller>) tbSellerMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 添加
     *
     * @param seller
     * @return
     */
    @Override
    public void add(TbSeller seller) {
        tbSellerMapper.insert(seller);
    }

    /**
     * 更改
     *
     * @param seller
     */
    @Override
    public void update(TbSeller seller) {
        tbSellerMapper.updateByPrimaryKeySelective(seller);
    }


    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbSeller findOne(String id) {
        return tbSellerMapper.selectByPrimaryKey(id);
    }

    /**
     * 删除
     *
     * @param ids
     */
    @Override
    public void delete(String[] ids) {
        tbSellerMapper.deleteByPrimaryKey(ids);
    }

    /**
     * 分页
     *
     * @param seller
     * @param pageNum  当前页 码
     * @param pageSize 每页记录数
     * @return
     */
    @Override
    public PageResult findPage(TbSeller seller, int pageNum, int pageSize) {
        //使用分页插件
        PageHelper.startPage(pageNum, pageSize);
        TbSellerExample sellerExample = new TbSellerExample();
        TbSellerExample.Criteria criteria = sellerExample.createCriteria();
        Page<TbSeller> page = (Page<TbSeller>) tbSellerMapper.selectByExample(sellerExample);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 更新状态
     *
     * @param sellerId
     * @param status
     */
    @Override
    public void updateStatus(String sellerId, String status) {
        TbSeller seller = tbSellerMapper.selectByPrimaryKey(sellerId);
        System.out.println(seller);
        seller.setStatus(status);
        tbSellerMapper.updateByPrimaryKeySelective(seller);
    }

    @Override
    public PageResult search(TbSeller tbSeller, int page, int rows) {
        return null;
    }
}
