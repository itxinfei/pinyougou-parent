package com.pinyougou.order.integration;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * 集成测试基类
 * <p>
 * 功能说明：
 * - 提供Spring上下文加载和事务管理
 * - 提供数据库操作的便捷方法
 * - 自动回滚测试数据，保证测试隔离性
 * <p>
 * 运行前提：
 * - 需要配置数据库（H2内存数据库或MySQL）
 * - 需要配置Spring上下文（spring-*.xml或applicationContext*.xml）
 * - 在没有数据库环境下，子类测试将被跳过
 * <p>
 * 事务策略：
 * - 使用@Transactional注解，每个测试方法运行在独立事务中
 * - 测试结束后自动回滚，不会污染数据库
 * <p>
 * 使用方式：
 * - 子类继承此类，添加@Autowired注入所需的Mapper或Service
 * - 使用提供的工具方法进行数据库操作验证
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
@Ignore("需要数据库环境，CI环境跳过")
public abstract class IntegrationTestBase {

    @Autowired
    protected DataSource dataSource;

    /**
     * 检查数据库表是否存在
     * <p>
     * 通过DatabaseMetaData查询表是否存在
     *
     * @param tableName 表名
     * @return true-表存在，false-表不存在
     */
    protected boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        } catch (Exception e) {
            fail("检查表存在性失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 查询表的行数
     * <p>
     * 执行SELECT COUNT(*)查询，返回表中的记录数
     *
     * @param tableName 表名
     * @return 行数，查询失败返回-1
     */
    protected long countTableRows(String tableName) {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
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
     * 执行SQL更新语句
     * <p>
     * 用于执行INSERT、UPDATE、DELETE等SQL语句
     *
     * @param sql SQL语句
     * @return 受影响的行数，执行失败返回-1
     */
    protected int executeUpdate(String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            fail("执行SQL失败: " + e.getMessage());
            return -1;
        }
    }
}
