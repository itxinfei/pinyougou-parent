package com.pinyougou.mapper;

import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TbGoodsMapper {
    int countByExample(TbGoodsExample example);

    int deleteByExample(TbGoodsExample example);

    int deleteByPrimaryKey(Long[] id);

    int insert(TbGoods record);

    int insertSelective(TbGoods record);

    List<TbGoods> selectByExample(TbGoodsExample example);

    TbGoods selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") TbGoods record, @Param("example") TbGoodsExample example);

    int updateByExample(@Param("record") TbGoods record, @Param("example") TbGoodsExample example);

    int updateByPrimaryKeySelective(TbGoods record);

    int updateByPrimaryKey(TbGoods record);

    /**
     * 根据模板ID查询品牌列表（用于搜索页缓存降级）
     * 关联查询 tb_item_cat 和 tb_brand 表
     * @param templateId 类型模板ID
     * @return 品牌列表 Map(id, text)
     */
    List<Map> queryBrandListByTemplateId(Long templateId);

    /**
     * 根据模板ID查询规格列表（用于搜索页缓存降级）
     * 从 tb_type_template 的 spec_ids 字段解析
     * @param templateId 类型模板ID
     * @return 规格列表 Map(id, name)
     */
    List<Map> querySpecListByTemplateId(Long templateId);
}