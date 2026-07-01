# 集成测试指南

## 概述

本项目使用H2内存数据库配置集成测试框架，支持测试完整的DAO层和Service层功能。

## 特性

- ✅ **零配置**：使用H2内存数据库，无需安装外部数据库
- ✅ **自动管理**：测试前后自动创建/销毁数据库
- ✅ **事务隔离**：每个测试方法自动回滚，保持数据清洁
- ✅ **快速执行**：内存数据库性能高，测试执行快
- ✅ **兼容MySQL**：H2支持MySQL模式，与生产环境一致

## 目录结构

```
src/test/
├── java/
│   └── com/pinyougou/
│       └── order/
│           ├── service/              # 单元测试
│           │   └── OrderServiceImplTest.java
│           └── integration/          # 集成测试
│               ├── IntegrationTestBase.java    # 基类
│               └── OrderServiceIntegrationTest.java
└── resources/
    ├── spring-test-datasource.xml   # H2数据源配置
    ├── schema.sql                   # 数据库表结构
    └── data.sql                     # 测试数据
```

## 快速开始

### 1. 编写集成测试

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath*:spring/spring-*.xml",
    "classpath*:applicationContext*.xml"
})
@Transactional  // 测试后自动回滚
public class UserServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private TbUserMapper userMapper;

    @Test
    public void testCreateUser() {
        // 测试代码
    }
}
```

### 2. 运行测试

**使用Maven：**
```bash
mvn test -Dtest=OrderServiceIntegrationTest
```

**使用IDE：**
- 右键点击测试类 → Run As → JUnit Test

### 3. 查看H2控制台（可选）

访问：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- 用户名: `sa`
- 密码: （空）

## 核心概念

### IntegrationTestBase

所有集成测试的基类，提供：

1. **自动配置Spring上下文**
   - 自动加载`classpath*:spring/spring-*.xml`
   - 自动加载`classpath*:applicationContext*.xml`

2. **数据源访问**
   ```java
   @Autowired
   protected DataSource dataSource;
   ```

3. **工具方法**
   - `tableExists(tableName)` - 检查表是否存在
   - `countTableRows(tableName)` - 统计表行数
   - `executeUpdate(sql)` - 执行SQL更新

4. **事务管理**
   - `@Transactional`注解确保测试后自动回滚
   - 每个测试方法都在独立事务中执行
   - 测试数据互不影响

### H2数据库配置

配置文件：`src/test/resources/spring-test-datasource.xml`

```xml
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="org.h2.Driver"/>
    <property name="url" value="jdbc:h2:mem:testdb;MODE=MySQL;INIT=runscript from 'classpath:schema.sql'"/>
    <property name="username" value="sa"/>
    <property name="password" value=""/>
</bean>
```

**关键参数说明：**
- `mem:testdb` - 内存数据库名称
- `MODE=MySQL` - 兼容MySQL语法
- `DB_CLOSE_DELAY=-1` - 保持数据库直到JVM关闭
- `INIT=runscript from 'classpath:schema.sql'` - 初始化时执行schema.sql

### Schema和数据初始化

**schema.sql**：定义数据库表结构
```sql
CREATE TABLE tb_order (
    order_id BIGINT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    ...
);
```

**data.sql**：插入测试数据（可选）
```sql
INSERT INTO tb_item VALUES (1001, 2001, '华为P50 Pro', 4999.00, 100, ...);
```

**自动加载顺序：**
1. Spring创建DataSource
2. H2自动执行schema.sql创建表
3. data.sql加载测试数据（如果配置了spring.datasource.initialization-mode=always）

## 最佳实践

### ✅ 推荐

1. **每个测试方法独立**
   ```java
   @Test
   public void testCreateUser() {
       // 创建新数据，不依赖其他测试
   }
   ```

2. **使用@Transactional确保数据清洁**
   ```java
   @Transactional
   public class MyIntegrationTest extends IntegrationTestBase {
       // ...
   }
   ```

3. **验证数据库状态**
   ```java
   assertEquals(1, countTableRows("tb_order"));
   assertTrue(tableExists("tb_order"));
   ```

4. **使用测试专用Mapper/Service**
   ```java
   @Autowired
   private TbOrderMapper orderMapper;
   ```

### ❌ 不推荐

1. **不要共享测试数据**
   ```java
   // ❌ 错误：依赖其他测试创建的数据
   @Test
   public void testUpdateUser() {
       userMapper.updateByPrimaryKey(existingUser); // 这个user从哪来？
   }
   ```

2. **不要手动管理事务**
   ```java
   // ❌ 错误：手动提交事务
   @Test
   public void testSomething() {
       TransactionStatus status = transactionManager.getTransaction(...);
       // ...
       transactionManager.commit(status);
   }
   ```

3. **不要使用@Rollback(false)**
   ```java
   // ❌ 错误：不自动回滚，污染数据库
   @Test
   @Transactional
   @Rollback(false)
   public void testSomething() {
       // ...
   }
   ```

## 常见问题

### Q1: 如何运行所有集成测试？

```bash
# 运行所有测试（单元测试 + 集成测试）
mvn test

# 仅运行集成测试
mvn test -Dtest=*IntegrationTest
```

### Q2: 如何调试集成测试？

在测试方法打断点，右键 → Debug As → JUnit Test

### Q3: 如何查看H2数据库内容？

启用H2控制台，访问 http://localhost:8080/h2-console

### Q4: 如何处理复杂的数据准备？

使用`@Sql`注解加载SQL脚本：
```java
@Test
@Sql(scripts = "/test-data/create-test-order.sql")
public void testWithCustomData() {
    // ...
}
```

### Q5: 集成测试和单元测试的区别？

| 特性 | 单元测试 | 集成测试 |
|------|---------|---------|
| 数据库 | Mock | H2真实数据库 |
| 速度 | 快 | 相对慢 |
| 隔离性 | 完全隔离 | 需要事务回滚 |
| 覆盖范围 | 单个类/方法 | 多层交互 |
| 推荐比例 | 70% | 30% |

## 进阶配置

### 使用MySQL替代H2

修改`spring-test-datasource.xml`：
```xml
<property name="driverClassName" value="com.mysql.jdbc.Driver"/>
<property name="url" value="jdbc:mysql://localhost:3306/testdb?useSSL=false"/>
<property name="username" value="root"/>
<property name="password" value="password"/>
```

### 自定义Schema路径

```java
@ContextConfiguration(locations = {
    "classpath*:spring/spring-*.xml",
    "classpath*:applicationContext*.xml"
})
@TestPropertySource(properties = {
    "spring.datasource.schema=classpath:custom-schema.sql"
})
```

## 参考资源

- [Spring Test Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html)
- [H2 Database Documentation](https://www.h2database.com/html/main.html)
- [JUnit 4 Documentation](https://junit.org/junit4/)
