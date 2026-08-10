package com.pinyougou.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.GenericMapper;
import entity.GenericService;
import entity.PageResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通用服务实现基类，提供标准 CRUD 操作的默认实现。
 * <p>
 * 子类只需实现 {@link #getMapper()} 方法返回对应的 Mapper 实例，
 * 即可自动获得 findAll、findPage、add、update、findOne、delete 六个标准方法。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * public class GoodsDescServiceImpl extends BaseServiceImpl&lt;TbGoodsDesc&gt;
 *         implements GoodsDescService {
 *
 *     &#64;Autowired
 *     private TbGoodsDescMapper goodsDescMapper;
 *
 *     &#64;Override
 *     protected GenericMapper&lt;TbGoodsDesc&gt; getMapper() {
 *         return goodsDescMapper;
 *     }
 * }
 * </pre>
 * </p>
 *
 * @param <T> 实体类型
 */
public abstract class BaseServiceImpl<T> implements GenericService<T> {

    /**
     * 获取当前服务对应的 Mapper 实例。
     * <p>
     * 子类必须实现此方法，返回通过 {@code @Autowired} 注入的 Mapper 对象。
     * </p>
     *
     * @return Mapper 实例
     */
    protected abstract GenericMapper<T> getMapper();

    /**
     * 查询全部记录
     *
     * @return 实体列表
     */
    @Override
    public List<T> findAll() {
        return getMapper().selectByExample(null);
    }

    /**
     * 分页查询（无搜索条件）
     *
     * @param pageNum  当前页码（从1开始）
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        @SuppressWarnings("unchecked")
        Page<T> page = (Page<T>) getMapper().selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 新增实体
     *
     * @param entity 实体对象
     */
    @Override
    @Transactional
    public void add(T entity) {
        getMapper().insert(entity);
    }

    /**
     * 更新实体
     *
     * @param entity 实体对象（必须包含主键值）
     */
    @Override
    @Transactional
    public void update(T entity) {
        getMapper().updateByPrimaryKey(entity);
    }

    /**
     * 根据主键查询单条记录
     *
     * @param id 主键ID
     * @return 实体对象，不存在时返回 null
     */
    @Override
    public T findOne(Long id) {
        return getMapper().selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     *
     * @param ids 主键ID数组
     */
    @Override
    @Transactional
    public void delete(Long[] ids) {
        for (Long id : ids) {
            getMapper().deleteByPrimaryKey(id);
        }
    }
}
