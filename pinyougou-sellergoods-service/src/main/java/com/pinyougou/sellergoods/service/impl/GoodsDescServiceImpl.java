package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.GenericMapper;
import com.pinyougou.mapper.TbGoodsDescMapper;
import com.pinyougou.pojo.TbGoodsDesc;
import com.pinyougou.pojo.TbGoodsDescExample;
import com.pinyougou.pojo.TbGoodsDescExample.Criteria;
import com.pinyougou.sellergoods.service.GoodsDescService;
import com.pinyougou.service.BaseServiceImpl;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 服务实现层
 * @author Administrator
 */
@Service
public class GoodsDescServiceImpl extends BaseServiceImpl<TbGoodsDesc> implements GoodsDescService {

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;

	@Override
	protected GenericMapper<TbGoodsDesc> getMapper() {
		return goodsDescMapper;
	}

	@Override
	public PageResult findPage(TbGoodsDesc goodsDesc, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);

		TbGoodsDescExample example = new TbGoodsDescExample();
		Criteria criteria = example.createCriteria();

		if (goodsDesc != null) {
			if (goodsDesc.getIntroduction() != null && goodsDesc.getIntroduction().length() > 0) {
				criteria.andIntroductionLike("%" + goodsDesc.getIntroduction() + "%");
			}
			if (goodsDesc.getSpecificationItems() != null && goodsDesc.getSpecificationItems().length() > 0) {
				criteria.andSpecificationItemsLike("%" + goodsDesc.getSpecificationItems() + "%");
			}
			if (goodsDesc.getCustomAttributeItems() != null && goodsDesc.getCustomAttributeItems().length() > 0) {
				criteria.andCustomAttributeItemsLike("%" + goodsDesc.getCustomAttributeItems() + "%");
			}
			if (goodsDesc.getItemImages() != null && goodsDesc.getItemImages().length() > 0) {
				criteria.andItemImagesLike("%" + goodsDesc.getItemImages() + "%");
			}
			if (goodsDesc.getPackageList() != null && goodsDesc.getPackageList().length() > 0) {
				criteria.andPackageListLike("%" + goodsDesc.getPackageList() + "%");
			}
			if (goodsDesc.getSaleService() != null && goodsDesc.getSaleService().length() > 0) {
				criteria.andSaleServiceLike("%" + goodsDesc.getSaleService() + "%");
			}
		}

		Page<TbGoodsDesc> page = (Page<TbGoodsDesc>) goodsDescMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}

}
