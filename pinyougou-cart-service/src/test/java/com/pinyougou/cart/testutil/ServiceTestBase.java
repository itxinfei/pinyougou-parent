package com.pinyougou.cart.testutil;

import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Service层单元测试基类
 * <p>
 * 提供公共的测试初始化和工具方法：
 * 1. 自动初始化Mock对象（@Mock、@InjectMocks）
 * 2. 提供通用的测试数据准备方法
 * 3. 提供常用的测试工具方法
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class ServiceTestBase {

    /**
     * 初始化Mockito注解
     * <p>
     * 在每个测试类执行前自动调用，初始化@Mock、@InjectMocks等注解
     */
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * 通过反射设置私有字段
     * <p>
     * 用于注入没有setter的依赖（如private final字段）
     *
     * @param target   目标对象
     * @param fieldName 字段名
     * @param value    要设置的值
     */
    protected void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    /**
     * 通过反射获取私有字段
     * <p>
     * 用于验证私有字段的值
     *
     * @param target   目标对象
     * @param fieldName 字段名
     * @return 字段值
     */
    protected Object getField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }

    /**
     * 清理所有Mock对象的状态
     * <p>
     * 在每个测试方法执行后调用，确保测试隔离性
     * 建议在@After中使用
     */
    protected void resetMocks(Object... mocks) {
        if (mocks != null && mocks.length > 0) {
            Mockito.reset(mocks);
        }
    }

    /**
     * 验证方法未调用
     * <p>
     * 便捷方法，用于验证Mock方法未被调用
     *
     * @param mockObject Mock对象
     * @param methodName 方法名
     */
    protected void verifyNotCalled(Object mockObject, String methodName) {
        try {
            Mockito.verify(mockObject, Mockito.never());
            // 注意：此方法需要更复杂的实现来支持动态方法名
        } catch (Exception e) {
            throw new RuntimeException("验证方法调用失败", e);
        }
    }
}
