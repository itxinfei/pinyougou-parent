package com.pinyougou.sellergoods.service;

import com.pinyougou.pojo.TbSeller;
import entity.PageResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 商家审核-服务层接口
 */
public interface SellerService {

    /**
     * 返回全部列表
     *
     * @return
     */
    public List<TbSeller> findAll();


    /**
     * 分页
     *
     * @return
     */
    public PageResult findPage(int pageNum, int pageSize);


    /**
     * 增加
     *
     * @return
     */
    public void add(TbSeller seller);


    /**
     * 修改
     */
    public void update(TbSeller seller);


    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    public TbSeller findOne(String id);


    /**
     * 批量删除
     *
     * @param ids
     */
    public void delete(String[] ids);

    /**
     * 分页
     *
     * @param pageNum  当前页 码
     * @param pageSize 每页记录数
     * @return
     */
    public PageResult findPage(TbSeller seller, int pageNum, int pageSize);

    /**
     * 更新状态
     *
     * @param sellerId
     * @param status
     */
    public void updateStatus(String sellerId, String status);

    /**
     * 公司名称+店铺名称搜索
     */
    public PageResult search(@RequestBody TbSeller tbSeller, int page, int rows);
}
