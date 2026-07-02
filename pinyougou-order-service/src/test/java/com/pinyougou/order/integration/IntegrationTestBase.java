package com.pinyougou.order.integration;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath*:spring/spring-*.xml",
    "classpath*:applicationContext*.xml"
})
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected DataSource dataSource;

    protected boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        } catch (Exception e) {
            fail("检查表存在性失败: " + e.getMessage());
            return false;
        }
    }

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
