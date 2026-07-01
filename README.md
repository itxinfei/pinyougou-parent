# 🛍️ 品优购网上商城 - 微服务学习Demo

<p align="center">
  <a href="https://gitee.com/itxinfei">
    <img alt="Gitee" src="https://img.shields.io/badge/作者-心飞为你飞-3c3?style=flat&logo=gitee&logoColor=white">
  </a>
  <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-green?style=flat">
  <img alt="JDK" src="https://img.shields.io/badge/JDK-1.8+-brightgreen?style=flat">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-3.6.3+-yellowgreen?style=flat">
  <img alt="Spring" src="https://img.shields.io/badge/Spring-4.2.5-b6ac?style=flat">
  <img alt="Dubbo" src="https://img.shields.io/badge/Dubbo-2.8.4-blue?style=flat">
</p>

<div align="center">
  <h3>🎯 基于 Spring + Dubbo 的 B2B2C 电商平台学习Demo</h3>
  <p>适合 Java 后端开发者学习微服务架构、分布式系统设计</p>
</div>

---

## 📖 项目简介

**品优购网上商城**是一个完整的 B2B2C 电商平台 Demo 项目，采用微服务架构设计，涵盖从商品管理到订单支付的全流程电商业务场景。

### 🎓 适合人群

- ✅ Java 后端开发初学者
- ✅ 想学习微服务架构的开发者
- ✅ 需要电商项目实战经验的人
- ✅ 计算机相关专业的学生

### 💡 学习价值

本项目涵盖了大量企业级开发中的核心技术点：

- 🔥 **微服务架构**: 服务拆分、RPC 远程调用
- 🔥 **分布式技术**: Redis 缓存、ActiveMQ 消息队列
- 🔥 **数据持久化**: MyBatis 框架、分页插件
- 🔥 **安全认证**: JWT Token、Spring Security
- 🔥 **搜索引擎**: Solr 全文检索
- 🔥 **工程化实践**: Maven 多模块管理、统一异常处理

---

## ✨ 核心特性

### 📦 完整的电商业务场景

| 功能模块 | 技术要点 | 学习价值 |
|---------|---------|---------|
| 🛍️ **商品管理** | 分类、品牌、规格、模板 | CRUD、级联操作 |
| 🔍 **商品搜索** | Solr 全文检索、高亮显示 | 搜索引擎集成 |
| 🛒 **购物车** | Redis 存储、购物车合并 | 缓存应用 |
| 📋 **订单管理** | 状态流转、订单取消/退款 | 分布式事务 |
| 💰 **支付集成** | 微信支付、回调处理 | 第三方接口集成 |
| ⚡ **秒杀活动** | Redis + DB 双重验证 | 高并发场景 |
| 📢 **内容管理** | CMS 广告管理、Redis 缓存 | 缓存策略 |
| 📄 **页面静态化** | Freemarker + ActiveMQ | 异步处理 |

### 🏗️ 分布式架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      Web 层 (Presentation)                    │
│  运营商后台 │ 商家后台 │ 用户中心 │ 购物车 │ 搜索 │ 门户    │
└───────────────────────────┬─────────────────────────────────┘
                            ↓ ↑ Dubbo RPC
┌─────────────────────────────────────────────────────────────┐
│                    Service 层 (Business Logic)                │
│  商品服务 │ 用户服务 │ 订单服务 │ 购物车 │ 搜索 │ 支付等   │
└───────────────────────────┬─────────────────────────────────┘
                            ↓ ↑ MyBatis
┌─────────────────────────────────────────────────────────────┐
│               DAO 层 (Data Access Object)                     │
│         Mapper 接口 + XML 映射文件                             │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              基础设施层 (MySQL + Redis + Solr + MQ)           │
└─────────────────────────────────────────────────────────────┘
```

### 🛠️ 技术栈详解

| 技术分类 | 技术选型 | 版本 | 学习重点 |
|---------|---------|------|---------|
| **基础框架** | Spring | 4.2.5 | IoC、AOP、事务管理 |
| **RPC 框架** | Dubbo | 2.8.4 | 服务注册发现、负载均衡 |
| **持久层** | MyBatis | 3.2.8 | ORM 框架、动态 SQL |
| **分页插件** | PageHelper | 4.0.0 | 物理分页 |
| **数据库** | MySQL | 5.1 | 关系型数据库 |
| **缓存** | Redis | 2.8.1 | 缓存策略、数据结构 |
| **搜索** | Solr | 4.10 | 全文检索、分词器 |
| **消息队列** | ActiveMQ | 5.11 | 异步处理、解耦 |
| **安全框架** | Spring Security | 4.1.0 | 认证授权 |
| **JWT** | JJWT | 0.9.0 | Token 认证 |
| **构建工具** | Maven | 3.6.3 | 多模块管理 |

---

## 📁 项目结构

### 🎯 分层架构

```
pinyougou-parent/                    # 父工程（聚合工程）
│
├── 📦 pinyougou-pojo/              # 实体类层
│   ├── TbUser.java                  # 用户实体
│   ├── TbOrder.java                 # 订单实体
│   ├── TbItem.java                  # 商品实体
│   ├── TbRefund.java                # 退款实体
│   └── ... (共 23 个实体类)
│
├── 🛠️ pinyougou-common/            # 公共工具层
│   ├── util/                        # 工具类
│   │   ├── IdWorker.java            # ID 生成器（雪花算法）
│   │   ├── JwtUtils.java            # JWT 工具类
│   │   └── HttpClient.java          # HTTP 客户端
│   └── exception/                   # 异常定义
│
├── 💾 pinyougou-dao/                # 数据访问层
│   ├── mapper/                      # Mapper 接口（24 个）
│   ├── resources/mapper/            # MyBatis XML（24 个）
│   └── TbOrderMapper.xml            # 复杂查询示例
│
├── 🔌 pinyougou-*-interface/        # 服务接口层（9 个模块）
│   ├── pinyougou-user-interface/    # 用户服务接口
│   ├── pinyougou-order-interface/   # 订单服务接口
│   ├── pinyougou-cart-interface/    # 购物车服务接口
│   └── ...
│
├── ⚙️ pinyougou-*-service/          # 服务实现层（9 个模块）
│   ├── pinyougou-user-service/      # 用户服务实现
│   ├── pinyougou-order-service/     # 订单服务实现
│   ├── pinyougou-cart-service/      # 购物车服务实现
│   └── ...
│
├── 🌐 pinyougou-*-web/              # Web 层（8 个模块）
│   ├── pinyougou-manager-web/       # 运营商后台（9101）
│   ├── pinyougou-shop-web/          # 商家后台（9106）
│   ├── pinyougou-user-web/          # 用户中心（9104）
│   ├── pinyougou-cart-web/          # 购物车服务
│   ├── pinyougou-search-web/        # 搜索服务
│   ├── pinyougou-portal-web/        # 门户前台
│   ├── pinyougou-pay-web/           # 支付服务
│   └── pinyougou-seckill-web/       # 秒杀服务
│
└── 📚 pinyougou-solr-util/          # Solr 工具类
```

### 📊 模块统计

- **总模块数**: 28 个 Maven 模块
- **Service 模块**: 9 个（商品、用户、订单、购物车、搜索、支付、内容、秒杀、页面）
- **Web 模块**: 8 个（运营后台、商家后台、用户中心等）
- **Interface 模块**: 9 个（对应 Service 的接口定义）
- **基础模块**: 4 个（pojo、common、dao、util）
- **数据库表**: 23 张表
- **实体类**: 23 个 POJO + 23 个 Example 查询类

### 🔌 服务端口分布

| 服务名称 | 端口 | 职责 |
|---------|------|------|
| 商品服务 | 9001 | 商品增删改查 |
| 广告服务 | 9002 | 内容广告管理 |
| 用户服务 | 9003 | 用户认证、登录 |
| 订单服务 | 9004 | 订单生命周期管理 |
| 购物车服务 | 9005 | 购物车操作 |
| 搜索服务 | 9006 | Solr 全文检索 |
| 页面服务 | 9007 | 商品详情页静态化 |
| 支付服务 | 9008 | 微信支付集成 |
| 秒杀服务 | 9009 | 秒杀活动处理 |
| 运营后台 | 9101 | 平台管理功能 |
| 门户前台 | 9103 | 广告展示 |
| 用户中心 | 9104 | 用户相关功能 |
| 搜索页面 | 9105 | 商品搜索界面 |
| 商家后台 | 9106 | 商家管理功能 |

---

## 🚀 快速开始

### 📋 环境准备

**必需软件：**

| 软件 | 版本 | 下载地址 |
|------|------|---------|
| JDK | 1.8+ | https://www.oracle.com/java/technologies/javase-jdk8-downloads.html |
| Maven | 3.6.3+ | https://maven.apache.org/download.cgi |
| MySQL | 5.7+ | https://dev.mysql.com/downloads/mysql/ |
| Redis | 3.0+ | https://redis.io/download |
| Solr | 4.10+ | https://archive.apache.org/dist/lucene/solr/ |
| ActiveMQ | 5.11+ | https://activemq.apache.org/components/classic/download/ |

**推荐 IDE：**
- IntelliJ IDEA（推荐）
- Eclipse

### 💻 安装步骤

#### 1️⃣ 克隆项目

```bash
git clone https://gitee.com/itxinfei/pinyougou-parent.git
cd pinyougou-parent
```

#### 2️⃣ 导入数据库

```bash
# 创建数据库
CREATE DATABASE pinyougoudb DEFAULT CHARACTER SET utf8;

# 导入脚本（使用 MySQL 客户端）
mysql -u root -p pinyougoudb < docs/pinyougoudb-v1.3.sql
```

> 💡 **数据库脚本位置**: `docs/pinyougoudb-v1.3.sql`

#### 3️⃣ 配置环境

修改各模块的配置文件：

```properties
# 数据库配置（所有模块都需要）
jdbc.driver=com.mysql.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/pinyougoudb?useUnicode=true&characterEncoding=utf8
jdbc.username=root
jdbc.password=your_password

# Redis 配置
redis.host=localhost
redis.port=6379
redis.password=

# Solr 配置
solr.url=http://localhost:8983/solr/pinyougou

# ActiveMQ 配置
brokerURL=tcp://localhost:61616
```

#### 4️⃣ 编译项目

```bash
# 编译所有模块（跳过测试）
mvn clean compile -DskipTests

# 安装到本地仓库
mvn clean install -DskipTests
```

✅ **编译成功标志**: 看到 `BUILD SUCCESS`

#### 5️⃣ 启动服务

**启动顺序很重要！**

```bash
# 1. 启动基础设施
Redis → MySQL → Solr → ActiveMQ

# 2. 启动 Service 层（Dubbo 服务提供者）
pinyougou-sellergoods-service    # 商品服务
pinyougou-user-service           # 用户服务
pinyougou-order-service          # 订单服务
pinyougou-cart-service           # 购物车服务
pinyougou-search-service         # 搜索服务
pinyougou-content-service        # 内容服务
pinyougou-page-service           # 页面服务
pinyougou-pay-service            # 支付服务
pinyougou-seckill-service        # 秒杀服务

# 3. 启动 Web 层（Dubbo 服务消费者）
pinyougou-manager-web            # 运营后台
pinyougou-shop-web               # 商家后台
pinyougou-user-web               # 用户中心
pinyougou-cart-web               # 购物车
pinyougou-search-web             # 搜索
pinyougou-portal-web             # 门户
```

> ⚠️ **提示**: 建议在 IntelliJ IDEA 中直接运行各个模块的 `Application` 启动类

---

## 📚 学习路径

### 🗺️ 推荐学习顺序

#### 第一阶段：基础入门（1-2 周）

```
✅ 1. 了解项目整体架构
   ├─ 阅读本文档
   ├─ 理解微服务架构思想
   └─ 熟悉 Maven 多模块项目结构

✅ 2. 学习基础模块
   ├─ pinyougou-pojo: 实体类设计
   ├─ pinyougou-common: 工具类使用
   └─ pinyougou-dao: MyBatis 基础

✅ 3. 完成第一个服务
   └─ pinyougou-sellergoods-service
      ├─ 理解接口与实现分离
      ├─ 学习 Dubbo 服务发布
      └─ 学习 MyBatis CRUD
```

#### 第二阶段：核心功能（2-3 周）

```
✅ 4. 用户认证与授权
   ├─ pinyougou-user-service
   ├─ JWT Token 认证
   └─ Spring Security 集成

✅ 5. 业务核心模块
   ├─ pinyougou-cart-service: 购物车（Redis 应用）
   ├─ pinyougou-order-service: 订单（状态机）
   └─ pinyougou-pay-service: 支付（第三方集成）

✅ 6. 搜索与缓存
   ├─ pinyougou-search-service: Solr 集成
   └─ Redis 缓存策略实践
```

#### 第三阶段：进阶提升（3-4 周）

```
✅ 7. 复杂业务场景
   ├─ 秒杀系统：高并发处理
   ├─ 内容管理：CMS + 缓存
   └─ 页面静态化：Freemarker + MQ

✅ 8. 分布式技术
   ├─ 分布式事务初步
   ├─ 服务降级与熔断
   └─ 接口幂等性设计
```

### 🎯 重点学习模块

| 模块 | 核心技术 | 难度 | 建议时间 |
|------|---------|------|---------|
| **商品服务** | MyBatis、Dubbo | ⭐⭐ | 2 天 |
| **用户服务** | JWT、Spring Security | ⭐⭐⭐ | 3 天 |
| **购物车** | Redis、分布式会话 | ⭐⭐⭐ | 3 天 |
| **订单服务** | 状态机、事务管理 | ⭐⭐⭐⭐ | 4 天 |
| **搜索服务** | Solr、分词器 | ⭐⭐⭐ | 3 天 |
| **支付服务** | 微信支付、回调 | ⭐⭐⭐ | 3 天 |
| **秒杀服务** | 高并发、锁机制 | ⭐⭐⭐⭐⭐ | 5 天 |

---

## 🗂️ 目录结构详解

### 核心包说明

```
src/main/java/com/pinyougou/
│
├── common/                    # 公共模块
│   ├── enums/                 # 枚举类
│   ├── exception/             # 自定义异常
│   └── util/                  # 工具类
│
├── mapper/                    # MyBatis Mapper 接口
│   ├── TbItemMapper.java      # 商品 Mapper
│   ├── TbOrderMapper.java     # 订单 Mapper
│   └── ...
│
├── pojo/                      # 实体类
│   ├── TbItem.java            # 商品实体
│   ├── TbOrder.java           # 订单实体
│   ├── TbRefund.java          # 退款实体
│   └── *Example.java          # 查询条件类
│
└── [service]/                 # 服务层（各业务模块）
    ├── service/               # 接口定义
    │   ├── ItemService.java
    │   ├── OrderService.java
    │   └── ...
    └── service/impl/          # 实现类
        ├── ItemServiceImpl.java
        ├── OrderServiceImpl.java
        └── ...
```

---

## 🛠️ 开发规范

### 代码规范

本项目严格遵循 **阿里巴巴 Java 开发手册**：

- ✅ 包名小写：`com.pinyougou.cart`
- ✅ 类名大驼峰：`CartServiceImpl`
- ✅ 方法名小驼峰：`findCartList()`
- ✅ 常量全大写：`MAX_RETRY_COUNT`
- ✅ 统一返回对象：`entity.Result`
- ✅ 统一异常处理
- ✅ 使用 Slf4j/Log4j 日志
- ✅ 禁止 `System.out.println`

### Git 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Type 说明：**

| Type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 用户注册功能` |
| `fix` | 修复 Bug | `fix: 修复订单金额计算错误` |
| `docs` | 文档更新 | `docs: 更新 README` |
| `refactor` | 代码重构 | `refactor: 重构订单服务` |
| `test` | 测试相关 | `test: 添加订单服务测试` |
| `chore` | 构建/工具 | `chore: 更新依赖版本` |
| `perf` | 性能优化 | `perf: 优化 Redis 查询` |
| `ci` | CI 配置 | `ci: 添加 GitHub Actions` |

---

## 📚 文档索引

### 📖 官方文档

| 文档 | 说明 | 链接 |
|------|------|------|
| 📘 [开发环境搭建](docs/环境搭建.md) | 完整的环境配置指南 | [查看](docs/环境搭建.md) |
| 📗 [数据库设计](docs/pinyougoudb-v1.3.sql) | 23 张表结构详解 | [查看](docs/pinyougoudb-v1.3.sql) |
| 📙 [项目审查报告](docs/project-review-report-2026-07-01.md) | 代码质量审查报告 | [查看](docs/project-review-report-2026-07-01.md) |

### 🔗 外部资源

- **Spring 官方文档**: https://docs.spring.io/spring-framework/docs/4.2.x/spring-framework-reference/
- **Dubbo 官方文档**: https://dubbo.apache.org/zh/docs/
- **MyBatis 官方文档**: https://mybatis.org/mybatis-3/zh/index.html
- **Redis 命令参考**: https://redis.io/commands

---

## 🔧 常见问题

### ❓ 编译报错

**Q: 编译时提示 "Could not resolve dependencies"**
```bash
# 解决方案：清除本地仓库缓存并重新下载
mvn dependency:purge-local-repository
mvn clean install -DskipTests -U
```

**Q: 提示 "端口被占用"**
```bash
# 查找占用端口的进程
netstat -ano | findstr :8080

# 结束进程（替换 PID）
taskkill /PID <进程ID> /F
```

### ❓ 数据库问题

**Q: 数据库连接失败**
- ✅ 检查 MySQL 服务是否启动
- ✅ 确认用户名密码正确
- ✅ 检查数据库 `pinyougoudb` 是否存在

### ❓ 缓存问题

**Q: Redis 连接失败**
```bash
# 检查 Redis 是否启动
redis-cli ping
# 应返回 PONG
```

### ❓ 搜索问题

**Q: Solr 初始化失败**
- ✅ 确认 Solr 服务启动成功
- ✅ 创建 core: `pinyougou`
- ✅ 检查 `solr-config.properties` 配置

---

## 🤝 贡献指南

这是一个学习 Demo 项目，欢迎 Fork 和学习！

### 如何贡献

1. **Fork 本项目**
2. **创建特性分支**: `git checkout -b feature/新功能名称`
3. **提交更改**: `git commit -m 'feat: 添加新功能'`
4. **推送分支**: `git push origin feature/新功能名称`
5. **提交 Pull Request**

### 贡献建议

- 🐛 报告 Bug
- 📝 改进文档
- 💡 提出新功能建议
- 🔧 提交代码优化

---

## 📝 更新日志

### 📅 2026-07-01 - v1.1.0

**🎉 重大更新：订单取消/退款功能**

**新增功能：**
- ✅ 订单取消（未付款订单）
- ✅ 退款申请（已付款订单）
- ✅ 退款审核（通过/拒绝）
- ✅ 退款记录查询
- ✅ 库存自动恢复
- ✅ Token 黑名单机制

**代码质量提升：**
- ✅ 修复 15 个代码问题
- ✅ 完善空指针检查
- ✅ 统一异常处理
- ✅ 优化代码规范

**文档完善：**
- ✅ 添加项目审查报告
- ✅ 优化 README 文档
- ✅ 补充数据库脚本

---

### 📅 2026-06-30 - v1.0.0

**🎊 项目初始版本**

**核心功能：**
- ✅ 商品管理（分类、品牌、规格）
- ✅ 用户管理（注册、登录、JWT 认证）
- ✅ 购物车（Redis 存储）
- ✅ 订单管理（状态流转）
- ✅ 支付集成（微信支付）
- ✅ 商品搜索（Solr）
- ✅ 秒杀活动
- ✅ 内容广告管理
- ✅ 页面静态化

**技术特性：**
- ✅ 微服务架构（Spring + Dubbo）
- ✅ 分布式缓存（Redis）
- ✅ 消息队列（ActiveMQ）
- ✅ 全文检索（Solr）
- ✅ 安全认证（Spring Security + JWT）

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证 - 查看 LICENSE 文件了解详情

---

## 👨‍💻 作者

**心飞为你飞** 💖

- 🐙 GitHub: [@itxinfei](https://github.com/itxinfei)
- 📚 Gitee: [@itxinfei](https://gitee.com/itxinfei)
- 📧 Email: [747011882@qq.com](mailto:747011882@qq.com)
- 💬 QQ群: [661543188](https://gitee.com/link?target=https%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Fk%3DgNgch-wCkfUu-QbI7DZSudrax2BN7vY0%26jump_from%3Dwebapi%26authKey%3DQHSRnxQvu%2Bh5S3AXGn%2FDSHrVPiFQAYEk6bSlCE1lS276SFjQAUagV4FG7bHf0OSM)

---

## ⭐ Star History

如果这个项目对你有帮助，请给个 **Star** ⭐ 支持一下！

你的 Star 是我持续更新的动力！🚀

---

<div align="center">
  <h3>🎓 开始学习之旅</h3>
  <p>从 <a href="#-快速开始">快速开始</a> 开始你的微服务学习之旅吧！</p>
  <br>
  <b>Made with ❤️ by <a href="https://gitee.com/itxinfei">心飞为你飞</a></b>
</div>
