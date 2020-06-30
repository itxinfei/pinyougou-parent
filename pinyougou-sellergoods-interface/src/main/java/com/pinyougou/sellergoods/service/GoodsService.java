package com.pinyougou.sellergoods.service;

import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.group.Goods;
import entity.PageResult;

import java.util.List;

/**
 * 服务层接口
 *
 * @author Administrator
 */
public interface GoodsService {

    /**
     * 返回全部列表
     *
     * @return
     */
    public List<TbGoods> findAll();


    /**
     * 返回分页列表
     *
     * @return
     */
    public PageResult findPage(int pageNum, int pageSize);


    /**
     * 增加
     */
    public void add(Goods goods);


    /**
     * 修改
     *
     * @param goods
     */
    public void update(Goods goods);


    /**
     * 批量删除
     *
     * @param ids
     */
    public void delete(Long[] ids);

    /**
     * 分页
     *
     * @param pageNum  当前页 码
     * @param pageSize 每页记录数
     * @return
     */
    public PageResult findPage(TbGoods goods, int pageNum, int pageSize);

    /**
     * 更改状态
     *
     * @param ids
     * @param status
     */
    public void updateStatus(Long[] ids, String status);


    /**
     * 根据SPU的ID集合查询SKU列表
     *
     * @param goodsIds
     * @param status
     * @return
     */
    public List<TbItem> findItemListByGoodsIdListAndStatus(Long[] goodsIds, String status);

    /**
     * 根据ID查询实体
	 * @return
	 */
    public TbGoods findById(Long id);
}
