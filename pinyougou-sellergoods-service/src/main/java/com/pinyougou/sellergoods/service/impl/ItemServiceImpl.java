package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.GenericMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojo.TbItemExample.Criteria;
import com.pinyougou.sellergoods.service.ItemService;
import com.pinyougou.service.BaseServiceImpl;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class ItemServiceImpl extends BaseServiceImpl<TbItem> implements ItemService {

    @Autowired
    private TbItemMapper itemMapper;

    @Override
    protected GenericMapper<TbItem> getMapper() {
        return itemMapper;
    }

    @Override
    public PageResult findPage(TbItem item, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbItemExample example = new TbItemExample();
        Criteria criteria = example.createCriteria();

        if (item != null) {
            if (item.getTitle() != null && item.getTitle().length() > 0) {
                criteria.andTitleLike("%" + item.getTitle() + "%");
            }
            if (item.getSellPoint() != null && item.getSellPoint().length() > 0) {
                criteria.andSellPointLike("%" + item.getSellPoint() + "%");
            }
            if (item.getBarcode() != null && item.getBarcode().length() > 0) {
                criteria.andBarcodeLike("%" + item.getBarcode() + "%");
            }
            if (item.getImage() != null && item.getImage().length() > 0) {
                criteria.andImageLike("%" + item.getImage() + "%");
            }
            if (item.getStatus() != null && item.getStatus().length() > 0) {
                criteria.andStatusLike("%" + item.getStatus() + "%");
            }
            if (item.getItemSn() != null && item.getItemSn().length() > 0) {
                criteria.andItemSnLike("%" + item.getItemSn() + "%");
            }
            if (item.getIsDefault() != null && item.getIsDefault().length() > 0) {
                criteria.andIsDefaultLike("%" + item.getIsDefault() + "%");
            }
            if (item.getSellerId() != null && item.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + item.getSellerId() + "%");
            }
            if (item.getCartThumbnail() != null && item.getCartThumbnail().length() > 0) {
                criteria.andCartThumbnailLike("%" + item.getCartThumbnail() + "%");
            }
            if (item.getCategory() != null && item.getCategory().length() > 0) {
                criteria.andCategoryLike("%" + item.getCategory() + "%");
            }
            if (item.getBrand() != null && item.getBrand().length() > 0) {
                criteria.andBrandLike("%" + item.getBrand() + "%");
            }
            if (item.getSeller() != null && item.getSeller().length() > 0) {
                criteria.andSellerLike("%" + item.getSeller() + "%");
            }

        }

        Page<TbItem> page = (Page<TbItem>) itemMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

}
