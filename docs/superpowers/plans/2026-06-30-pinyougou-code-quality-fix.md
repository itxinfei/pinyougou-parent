# 品优购代码质量修复 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复品优购项目中全部功能缺陷、安全漏洞、代码质量问题，遵循阿里巴巴Java代码规范

**架构:** Spring 4.2.5 + Dubbo 2.8.4 + MyBatis 3.2.8 + Redis + Solr + ActiveMQ，Maven多模块分布式项目

**Tech Stack:** Spring 4.2.5 / Dubbo 2.8.4 / MyBatis 3.2.8 / MySQL 5.1 / Redis / Solr 4.10 / ActiveMQ 5.11

## Global Constraints

- 禁止升级框架版本和JDK版本（保持 Java 1.8 / Spring 4.2.5 / Dubbo 2.8.4）
- 所有代码必须符合阿里巴巴Java代码规范
- 不改动 MyBatis Generator 自动生成的文件
- 不改动数据库表结构
- 不改动 POM 中的框架版本依赖
- Controller统一返回 `entity.Result`（success + message）或 `entity.PageResult`（total + rows）

---
## 文件结构概览

### 功能修复文件
| 文件 | 修改说明 |
|------|---------|
| `pinyougou-sellergoods-service/.../GoodsServiceImpl.java` | 实现 stub 方法（findPage/findById/findItemListByGoodsIdListAndStatus） |
| `pinyougou-manager-web/.../GoodsController.java` | 实现 delete/updateStatus/updateIsDelete 逻辑 |
| `pinyougou-search-web/.../ItemSearchController.java` | 消除双重搜索调用 |
| `pinyougou-order-service/.../OrderServiceImpl.java` | BigDecimal 精度修复 + 库存原子扣减 |
| `pinyougou-cart-service/.../CartServiceImpl.java` | BigDecimal 精度修复 |
| `pinyougou-pojo/.../entity/Result.java` | 删除重复 flag 字段 |
| `pinyougou-common/.../exception/BusinessException.java` | 消除 message 字段覆盖父类问题 |
| `pinyougou-page-service/.../ItemPageServiceImpl.java` | 添加 null 检查 |
| `pinyougou-content-service/.../ContentServiceImpl.java` | 恢复 @Transactional 注解 |
| `pinyougou-dao/.../TbItemMapper.java` | 添加 `decreaseStockCount` 方法 |
| `pinyougou-dao/.../TbItemMapper.xml` | 添加原子扣减库存 SQL |

### 安全加固文件
| 文件 | 修改说明 |
|------|---------|
| `pinyougou-manager-web/.../spring-security.xml` | 使用 BCryptPasswordEncoder，编码密码 |
| `pinyougou-shop-web/.../spring-security.xml` | 同上 |
| `pinyougou-cart-web/.../spring-security.xml` | 同上 |
| `pinyougou-seckill-web/.../spring-security.xml` | 同上 |
| `pinyougou-user-web/.../spring-security.xml` | 同上 |

### 代码质量文件（全部Java源文件，按模块分组处理）

---

### Task 1: 修复 Result.java 双布尔字段

**Files:**
- Modify: `pinyougou-pojo/src/main/java/entity/Result.java`

- [ ] **删除 `flag` 字段及相关方法**

```java
package entity;

import java.io.Serializable;

public class Result implements Serializable {

    private boolean success;
    private String message;

    public Result(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
```

- [ ] **验证编译**
Run: `mvn compile -pl pinyougou-pojo -q`

### Task 2: 修复 BusinessException.java message 字段覆盖

**Files:**
- Modify: `pinyougou-common/src/main/java/exception/BusinessException.java`

- [ ] **删除局部 `message` 字段，使用父类 getMessage()**

```java
package exception;

public class BusinessException extends RuntimeException {
    private String code;

    public BusinessException(String message) {
        super(message);
        this.code = "500";
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
```

### Task 3: 实现 GoodsServiceImpl.java stub 方法

**Files:**
- Modify: `pinyougou-sellergoods-service/src/main/java/com/pinyougou/sellergoods/service/impl/GoodsServiceImpl.java`

- [ ] **F1: 恢复 findPage 方法中被注释的分页查询逻辑**

Uncomment and fix the logic in `findPage(TbGoods, int, int)`:

```java
@Override
public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
    PageHelper.startPage(pageNum, pageSize);

    TbGoodsExample example = new TbGoodsExample();
    TbGoodsExample.Criteria criteria = example.createCriteria();

    criteria.andIsDeleteIsNull();

    if (goods != null) {
        if (goods.getSellerId() != null && goods.getSellerId().length() > 0) {
            criteria.andSellerIdEqualTo(goods.getSellerId());
        }
        if (goods.getGoodsName() != null && goods.getGoodsName().length() > 0) {
            criteria.andGoodsNameLike("%" + goods.getGoodsName() + "%");
        }
        if (goods.getAuditStatus() != null && goods.getAuditStatus().length() > 0) {
            criteria.andAuditStatusLike("%" + goods.getAuditStatus() + "%");
        }
        if (goods.getIsMarketable() != null && goods.getIsMarketable().length() > 0) {
            criteria.andIsMarketableLike("%" + goods.getIsMarketable() + "%");
        }
        if (goods.getCaption() != null && goods.getCaption().length() > 0) {
            criteria.andCaptionLike("%" + goods.getCaption() + "%");
        }
        if (goods.getSmallPic() != null && goods.getSmallPic().length() > 0) {
            criteria.andSmallPicLike("%" + goods.getSmallPic() + "%");
        }
        if (goods.getIsEnableSpec() != null && goods.getIsEnableSpec().length() > 0) {
            criteria.andIsEnableSpecLike("%" + goods.getIsEnableSpec() + "%");
        }
        if (goods.getIsDelete() != null && goods.getIsDelete().length() > 0) {
            criteria.andIsDeleteLike("%" + goods.getIsDelete() + "%");
        }
    }

    Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(example);
    return new PageResult(page.getTotal(), page.getResult());
}
```

- [ ] **F2: 实现 findById 方法**

```java
@Override
public TbGoods findById(Long id) {
    return goodsMapper.selectByPrimaryKey(id);
}
```

- [ ] **F3: 实现 findItemListByGoodsIdListAndStatus**

```java
@Override
public List<TbItem> findItemListByGoodsIdListAndStatus(Long[] goodsIds, String status) {
    TbItemExample example = new TbItemExample();
    TbItemExample.Criteria criteria = example.createCriteria();
    criteria.andGoodsIdIn(java.util.Arrays.asList(goodsIds));
    criteria.andStatusEqualTo(status);
    return itemMapper.selectByExample(example);
}
```

### Task 4: 实现 GoodsController.java 空方法

**Files:**
- Modify: `pinyougou-manager-web/src/main/java/com/pinyougou/manager/controller/GoodsController.java`

- [ ] **F4: 实现 delete / updateStatus / updateIsDelete**

```java
@RequestMapping("/delete")
public Result delete(final Long[] ids) {
    try {
        goodsService.delete(ids);
        return new Result(true, "删除成功");
    } catch (Exception e) {
        return new Result(false, "删除失败");
    }
}

@RequestMapping("/updateStatus")
public Result updateStatus(final Long[] ids, String status) {
    try {
        goodsService.updateStatus(ids, status);
        return new Result(true, "修改成功");
    } catch (Exception e) {
        return new Result(false, "修改失败");
    }
}

@RequestMapping("/updateIsDelete")
public Result updateIsDelete(final Long[] ids) {
    try {
        goodsService.delete(ids);
        return new Result(true, "操作成功");
    } catch (Exception e) {
        return new Result(false, "操作失败");
    }
}
```

### Task 5: 修复 ItemSearchController 双重搜索调用

**Files:**
- Modify: `pinyougou-search-web/src/main/java/com/pinyougou/search/controller/ItemSearchController.java`

- [ ] **F5: 消除重复 search 调用**

```java
@RequestMapping("/search")
public Map search(@RequestBody Map searchMap) {
    return itemSearchService.search(searchMap);
}
```

### Task 6: 修复 BigDecimal 精度问题（CartServiceImpl + OrderServiceImpl）

**Files:**
- Modify: `pinyougou-cart-service/src/main/java/com/pinyougou/cart/service/impl/CartServiceImpl.java`
- Modify: `pinyougou-order-service/src/main/java/com/pinyougou/order/service/impl/OrderServiceImpl.java`

- [ ] **F6a: 修复 CartServiceImpl.java 中 BigDecimal(double) 问题**

Line 86: `new BigDecimal(orderItem.getPrice().doubleValue()*orderItem.getNum())`
→ `orderItem.getPrice().multiply(new BigDecimal(orderItem.getNum()))`

Line 146: `new BigDecimal(item.getPrice().doubleValue()*num)`
→ `item.getPrice().multiply(new BigDecimal(num))`

- [ ] **F6b: 修复 OrderServiceImpl.java 中 BigDecimal(double) 问题**

Line 162: `tbOrder.setPayment(new BigDecimal(money))`
→ `tbOrder.setPayment(BigDecimal.valueOf(money))`

### Task 7: 修复库存扣减线程安全

**Files:**
- Modify: `pinyougou-dao/src/main/java/com/pinyougou/mapper/TbItemMapper.java`
- Modify: `pinyougou-dao/src/main/resources/com/pinyougou/mapper/TbItemMapper.xml`
- Modify: `pinyougou-order-service/src/main/java/com/pinyougou/order/service/impl/OrderServiceImpl.java`

- [ ] **F7a: 添加 `decreaseStockCount` 方法到 TbItemMapper**

```java
int decreaseStockCount(@Param("itemId") Long itemId, @Param("num") Integer num);
```

- [ ] **F7b: 添加 SQL 到 TbItemMapper.xml**

```xml
<update id="decreaseStockCount">
    update tb_item
    set stock_count = stock_count - #{num}
    where id = #{itemId} and stock_count >= #{num}
</update>
```

- [ ] **F7c: 修改 OrderServiceImpl 使用原子扣减**

Replace lines 143-153:
```java
for (TbOrderItem orderItem : cart.getOrderItemList()) {
    TbItem item = itemMap.get(orderItem.getItemId());
    if (item == null) {
        throw new ResourceNotFoundException("商品不存在，商品ID：" + orderItem.getItemId());
    }
    int result = itemMapper.decreaseStockCount(orderItem.getItemId(), orderItem.getNum());
    if (result == 0) {
        throw new InsufficientStockException("库存不足，商品：" + item.getTitle());
    }

    orderItem.setId(idWorker.nextId());
    orderItem.setOrderId(orderId);
    orderItem.setSellerId(cart.getSellerId());
    orderItemMapper.insert(orderItem);
    money += orderItem.getTotalFee().doubleValue();
}
```

### Task 8: 修复 ItemPageServiceImpl 空指针风险

**Files:**
- Modify: `pinyougou-page-service/src/main/java/com/pinyougou/page/service/impl/ItemPageServiceImpl.java`

- [ ] **F10: 添加 goods null 检查**

After line 70 (`TbGoods goods = goodsMapper.selectByPrimaryKey(goodsId);`):
```java
if (goods == null) {
    return false;
}
```

After line 73 (`TbGoodsDesc goodsDesc = goodsDescMapper.selectByPrimaryKey(goodsId);`):
```java
if (goodsDesc == null) {
    return false;
}
```

### Task 9: 恢复 ContentServiceImpl 事务注解

**Files:**
- Modify: `pinyougou-content-service/src/main/java/com/pinyougou/content/service/impl/ContentServiceImpl.java`

- [ ] **F11: 恢复 @Transactional**

```java
@Service
@Transactional
public class ContentServiceImpl implements ContentService {
```

Also remove line 150's `System.out.println`:
Replace `System.out.println("从数据库中查询数据并放入缓存 ");` with logger.

### Task 10: 替换项目中全部 e.printStackTrace() 为 Logger

**Files:** 全项目约75处

- [ ] **Q1: 批量替换所有 e.printStackTrace() → logger.error()**

Pattern for replacement:
```java
// Before:
e.printStackTrace();

// After:
logger.error("操作失败", e);
```

For each Java file, add Logger field at class level if not present:
```java
import org.apache.log4j.Logger;

private static final Logger logger = Logger.getLogger(XxxClass.class);
```

### Task 11: 替换项目中全部 System.out.println 为 Logger

**Files:** 全项目约31处

- [ ] **Q2: 批量替换所有 System.out.println → logger.info/logger.debug**

Pattern:
```java
// Before:
System.out.println("消息内容");

// After:
logger.info("消息内容");
```

### Task 12: 修复空 catch 块

**Files:** 全项目约11处空 catch

- [ ] **Q3: 替换空 catch + e.printStackTrace 为正确日志**

Pattern:
```java
// Before:
catch (Exception e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
}

// After:
catch (Exception e) {
    logger.error("操作失败", e);
}
```

### Task 13: 安全 - 替换硬编码密码为 BCryptPasswordEncoder

**Files:**
- `pinyougou-manager-web/src/main/resources/spring/spring-security.xml`
- `pinyougou-shop-web/src/main/resources/spring/spring-security.xml`
- `pinyougou-cart-web/src/main/resources/spring/spring-security.xml`
- `pinyougou-seckill-web/src/main/resources/spring/spring-security.xml`
- `pinyougou-user-web/src/main/resources/spring/spring-security.xml`

- [ ] **S1: 替换 manager-web spring-security.xml**

```xml
<beans:beans xmlns="http://www.springframework.org/schema/security"
             xmlns:beans="http://www.springframework.org/schema/beans"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                        http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security.xsd">

    <http pattern="/*.html" security="none"/>
    <http pattern="/css/**" security="none"/>
    <http pattern="/img/**" security="none"/>
    <http pattern="/js/**" security="none"/>
    <http pattern="/plugins/**" security="none"/>

    <http use-expressions="false">
        <intercept-url pattern="/**" access="ROLE_ADMIN"/>
        <form-login login-page="/login.html" default-target-url="/admin/index.html"
                    authentication-failure-url="/login.html" always-use-default-target="true"/>
        <csrf disabled="true"/>
        <headers>
            <frame-options policy="SAMEORIGIN"/>
        </headers>
        <logout logout-success-url="/login.html"/>
    </http>

    <authentication-manager>
        <authentication-provider>
            <password-encoder ref="passwordEncoder"/>
            <user-service>
                <user name="admin" password="$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" authorities="ROLE_ADMIN"/>
                <user name="pangzhao" password="$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" authorities="ROLE_ADMIN"/>
            </user-service>
        </authentication-provider>
    </authentication-manager>

    <beans:bean id="passwordEncoder" class="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder"/>
</beans:beans>
```

- [ ] **S2: 对其他 spring-security.xml 做相同处理**（添加 password-encoder 和 BCryptPasswordEncoder bean）

### Task 14: 修复 sellergoods-service 编译器版本

**Files:**
- Modify: `pinyougou-sellergoods-service/pom.xml`

- [ ] **将 compiler source/target 从 7 改为 8**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.2</version>
    <configuration>
        <source>8</source>
        <target>8</target>
    </configuration>
</plugin>
```

### Task 15: 验证编译

- [ ] **全量编译验证**

```bash
mvn clean compile -q
```
Expected: BUILD SUCCESS

- [ ] **Commit 所有改动**

```bash
git add -A
git commit -m "refactor: 修复代码质量和功能缺陷

- 实现 GoodsServiceImpl 空方法 (findPage/findById/findItemListByGoodsIdListAndStatus)
- 实现 GoodsController 空方法 (delete/updateStatus/updateIsDelete)
- 消除 ItemSearchController 双重搜索调用
- 修复 BigDecimal(double) 精度问题
- 修复库存扣减线程安全，使用原子 SQL
- 修复 Result 双布尔字段
- 修复 BusinessException 字段覆盖
- 替换全部 e.printStackTrace() → Logger
- 替换全部 System.out.println → Logger
- 修复空 catch 块
- 替换硬编码密码为 BCryptPasswordEncoder
- 修复 compiler 版本为 1.8"
```
