package com.pinyougou.mapper;

import com.pinyougou.pojo.TbRefund;
import com.pinyougou.pojo.TbRefundExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TbRefundMapper {
    long countByExample(TbRefundExample example);

    int deleteByExample(TbRefundExample example);

    int deleteByPrimaryKey(Long id);

    int insert(TbRefund record);

    int insertSelective(TbRefund record);

    List<TbRefund> selectByExample(TbRefundExample example);

    TbRefund selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") TbRefund record, @Param("example") TbRefundExample example);

    int updateByExample(@Param("record") TbRefund record, @Param("example") TbRefundExample example);

    int updateByPrimaryKeySelective(TbRefund record);

    int updateByPrimaryKey(TbRefund record);
}
