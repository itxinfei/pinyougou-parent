package entity;

import java.util.List;

/**
 * 通用服务接口，定义所有业务服务共享的标准 CRUD 方法签名。
 * <p>
 * 各模块的 Service 接口（如 BrandService、UserService 等）应继承此接口，
 * 以统一服务层 API 规范。
 * </p>
 *
 * @param <T> 实体类型
 */
public interface GenericService<T> {

    /**
     * 查询全部记录
     *
     * @return 实体列表
     */
    List<T> findAll();

    /**
     * 分页查询（无搜索条件）
     *
     * @param pageNum  当前页码（从1开始）
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    PageResult findPage(int pageNum, int pageSize);

    /**
     * 新增实体
     *
     * @param entity 实体对象
     */
    void add(T entity);

    /**
     * 更新实体
     *
     * @param entity 实体对象（必须包含主键值）
     */
    void update(T entity);

    /**
     * 根据主键查询单条记录
     *
     * @param id 主键ID
     * @return 实体对象，不存在时返回 null
     */
    T findOne(Long id);

    /**
     * 批量删除
     *
     * @param ids 主键ID数组
     */
    void delete(Long[] ids);
}
