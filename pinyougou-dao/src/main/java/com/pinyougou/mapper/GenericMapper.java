package com.pinyougou.mapper;

import java.util.List;

/**
 * 通用 Mapper 接口，定义所有 Mapper 共享的标准 CRUD 方法。
 * <p>
 * 所有由 MyBatis Generator 生成的 Mapper 接口都应继承此接口，
 * 以便配合 {@link com.pinyougou.service.BaseServiceImpl} 实现通用服务层。
 * </p>
 *
 * @param <T> 实体类型
 */
public interface GenericMapper<T> {

    /**
     * 查询所有记录
     *
     * @return 实体列表
     */
    List<T> selectByExample(Object example);

    /**
     * 根据主键查询单条记录
     *
     * @param id 主键ID
     * @return 实体对象，不存在时返回 null
     */
    T selectByPrimaryKey(Long id);

    /**
     * 插入一条记录
     *
     * @param record 实体对象
     * @return 受影响行数
     */
    int insert(T record);

    /**
     * 根据主键更新一条记录
     *
     * @param record 实体对象（必须包含主键值）
     * @return 受影响行数
     */
    int updateByPrimaryKey(T record);

    /**
     * 根据主键删除一条记录
     *
     * @param id 主键ID
     * @return 受影响行数
     */
    int deleteByPrimaryKey(Long id);
}
