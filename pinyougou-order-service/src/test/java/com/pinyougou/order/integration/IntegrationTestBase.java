package com.pinyougou.order.integration;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.Assert.*;

/**
 * 集成测试基类
 * <p>
 * 配置说明：
 * 1. 使用Spring Test Context Framework加载ApplicationContext
 * 2. 使用H2内存数据库
 * 3. 每个测试方法执行后自动回滚，保持数据库清洁
 * 4. 支持Web环境测试（@WebAppConfiguration）
 *
 * 使用示例：
 * <pre>
 * {@literal @}RunWith(SpringJUnit4ClassRunner.class)
 * {@literal @}ContextConfiguration(locations = {"classpath*:spring-*.xml"})
 * {@literal @}Transactional
 * public class UserServiceIntegrationTest extends IntegrationTestBase {
 *
 *     {@literal @}Autowired
 *     private UserService userService;
 *
 *     {@literal @}Test
 *     public void testFindUser() {
 *         // 测试代码
 *     }
 * }
 * </pre>
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath*:spring/spring-*.xml",
    "classpath*:applicationContext*.xml"
})
@Transactional
public abstract class IntegrationTestBase {

    /**
     * 数据源（H2内存数据库）
     * <p>
     * 用于执行原生SQL或验证数据库状态
     */
    @Autowired
    protected DataSource dataSource;

    /**
     * 验证数据库表是否存在
     *
     * @param tableName 表名
     * @return 是否存在
     */
    protected boolean tableExists(String tableName) {
        try (var connection = dataSource.getConnection();
             var rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        } catch (Exception e) {
            fail("检查表存在性失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证数据库表行数
     *
     * @param tableName 表名
     * @return 行数
     */
    protected long countTableRows(String tableName) {
        try (var connection = dataSource.getConnection();
             var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (Exception e) {
            fail("查询表行数失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 执行SQL查询
     *
     * @param sql SQL语句
     * @return 查询结果的行数
     */
    protected int executeUpdate(String sql) {
        try (var connection = dataSource.getConnection();
             var stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            fail("执行SQL失败: " + e.getMessage());
            return -1;
        }
    }
}
