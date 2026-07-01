# 品优购项目代码质量修复设计文档

## 概述

品优购（pinyougou-parent）是一个基于 Spring + Dubbo + MyBatis 的 B2B2C 电商平台，共 28 个 Maven 模块，162 个 Java 文件。本项目旨在修复代码中存在的功能缺陷、安全漏洞、代码质量问题，并补充测试覆盖。

## 修复范围

### Phase 1：功能修复

| 编号 | 问题 | 文件位置 | 修复方案 |
|------|------|---------|---------|
| F1 | `findPage` 返回 null | `GoodsServiceImpl.java:229` | 恢复被注释的分页查询逻辑：使用 `TbGoodsExample` + `PageHelper.startPage()` + `goodsMapper.selectByExample()` |
| F2 | `findById` 返回 null | `GoodsServiceImpl.java:249` | 实现 `goodsMapper.selectByPrimaryKey(id)` 查询 |
| F3 | `findItemListByGoodsIdListAndStatus` 返回 null | `GoodsServiceImpl.java:244` | 在 `TbItemMapper` 中添加自定义查询方法，使用 `IN (goodsIds)` + `status=#{status}` 条件 |
| F4 | 3个Controller方法返回 null | `manager/GoodsController.java:83-123` | `delete` → 调用 service 批量逻辑删除；`updateStatus` → 调用 service 更新审核状态；`updateIsDelete` → 调用 service 更新删除标记 |
| F5 | 双重搜索调用 | `ItemSearchController.java:30-32` | 删除第一次用于调试的 search 调用，仅保留一次并返回结果 |
| F6 | BigDecimal 精度风险 | `OrderServiceImpl.java:162`, `CartServiceImpl.java:86` | `new BigDecimal(double)` → `BigDecimal.valueOf(double)` |
| F7 | 库存扣减非线程安全 | `OrderServiceImpl.java:144-150` | 在 `TbItemMapper` 中添加 `updateStockCount(itemId, delta)` 方法，SQL: `UPDATE tb_item SET stock_count = stock_count - #{num} WHERE id=#{itemId} AND stock_count >= #{num}` |
| F8 | Result 双布尔字段 | `entity/Result.java` | 删除 `flag` 字段及 `isFlag()` 方法，仅保留 `success` 字段 |
| F9 | BusinessException 覆盖父类 message | `BusinessException.java` | 删除类中定义的 `message` 字段，`getMessage()` 直接返回 `super.getMessage()` |
| F10 | 页面生成无 null 检查 | `ItemPageServiceImpl.java:70` | `goods` 查询后添加 null 检查，若为 null 则直接返回 false |
| F11 | @Transactional 被注释 | `ContentServiceImpl.java` | 恢复 `@Transactional` 注解 |

### Phase 2：安全加固

| 编号 | 问题 | 文件位置 | 修复方案 |
|------|------|---------|---------|
| S1 | 硬编码密码 | `manager-web/spring-security.xml:33-35` | 使用 `BCryptPasswordEncoder` 编码密码；使用 Spring Security 的 `password-encoder` 引用 |
| S2 | 其他 security 配置 | shop-web, cart-web, seckill-web, user-web 的 spring-security.xml | 统一添加 `password-encoder` 配置 |

### Phase 3：代码质量清理

| 编号 | 问题 | 修复方案 |
|------|------|---------|
| Q1 | e.printStackTrace() ×75 处 | 替换为 `Logger.error("描述信息", e)`，引入 SLF4J Logger |
| Q2 | System.out.println ×31 处 | 替换为对应的 `Logger.info/debug/warn` |
| Q3 | 空 catch + TODO ×11 处 | 补充异常日志，或抛出上层异常 |
| Q4 | 注释代码 | 清理 GoodsServiceImpl.findPage 中注释代码、ContentServiceImpl 中注释 |

### Phase 4：测试覆盖

为核心 Service 层添加 JUnit 测试：
- BrandServiceImplTest — CRUD + 分页查询
- GoodsServiceImplTest — 商品创建 + 查询
- OrderServiceImplTest — 订单创建流程
- CartServiceImplTest — 购物车添加 + 合并

## 修复策略

1. **Phase 1 和 Phase 3 可并行执行**（功能修复和代码质量清理互不冲突）
2. **Phase 2 在 Phase 1 之后**（安全配置改动可能影响 Phase 1 测试）
3. **Phase 4 最后执行**（功能修完后写测试才有意义）
4. 每个 Phase 内使用并行子代理处理独立任务

## 接口约定

- 所有 Controller 返回 `entity.Result` 对象（`success: boolean, message: String`）
- 分页接口返回 `entity.PageResult`（`total: long, rows: List<?>`）
- Service 接口通过 Dubbo `@Service` 暴露，Controller 通过 `@Reference` 引用

## YAGNI

- 不重构整体架构（保持 Spring 4 + Dubbo + MyBatis 现有架构）
- 不替换日志框架（仅在现有 log4j 基础上使用 Logger）
- 不改动数据库表结构
- 不改动 MyBatis Generator 生成的文件

## 质量验收标准

1. 项目可通过 `mvn clean compile` 无报错编译
2. 所有之前返回 null 的 stub 方法都有真实实现
3. 无 `e.printStackTrace()` 和 `System.out.println` 遗留
4. 无硬编码密码（使用配置文件或 BCrypt 编码）
5. 核心 Service 层测试覆盖率达到 60%+
