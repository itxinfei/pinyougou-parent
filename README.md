# 品优购网上商城

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

## 📖 项目介绍

**品优购网上商城**是一个综合性的 B2B2C 电商平台，类似京东商城、天猫商城。网站采用商家入驻的模式，商家入驻平台提交申请，由平台进行资质审核，审核通过后商家拥有独立的管理后台录入商品信息。商品经过平台审核后即可发布。

品优购网上商城主要分为三个子系统：

- 🛒 **网站前台** - 面向普通消费者，提供商品浏览、购物车、订单支付等功能
- 🏢 **运营商后台** - 平台管理员管理商品分类、品牌、广告、商家审核等
- 🏪 **商家管理后台** - 商家管理自己的商品、订单、库存等

## 🎯 项目特性

✅ **完整的电商业务场景**
- 商品管理（分类、品牌、规格、模板）
- 商品搜索（基于 Solr 全文检索）
- 购物车功能
- 订单管理（待付款、待发货、待收货、已完成、已关闭）
- 支付集成（微信支付）
- 秒杀活动
- 内容广告管理（CMS）

✅ **分布式架构设计**
- 基于 Spring + Dubbo 的微服务架构
- 28 个 Maven 模块，清晰的职责划分
- 服务层与 Web 层分离
- 基于 Redis 的缓存优化
- 基于 ActiveMQ 的消息队列异步处理

✅ **完整的技术栈**
- **后端框架**: Spring 4.2.5 + Dubbo 2.8.4
- **持久层**: MyBatis 3.2.8 + PageHelper
- **数据库**: MySQL 5.1
- **缓存**: Redis
- **搜索**: Solr 4.10
- **消息队列**: ActiveMQ 5.11
- **安全框架**: Spring Security
- **构建工具**: Maven 3.6.3+

## 🏗️ 项目架构

### 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Web 层 (Presentation)                    │
│  pinyougou-manager-web  │  pinyougou-shop-web               │
│  pinyougou-user-web    │  pinyougou-cart-web               │
│  pinyougou-search-web  │  pinyougou-order-web              │
│  pinyougou-pay-web     │  pinyougou-seckill-web            │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑ Dubbo RPC
┌─────────────────────────────────────────────────────────────┐
│                    Service 层 (Business Logic)                │
│  pinyougou-sellergoods-service  │  pinyougou-user-service    │
│  pinyougou-order-service        │  pinyougou-cart-service    │
│  pinyougou-search-service       │  pinyougou-pay-service     │
│  pinyougou-page-service         │  pinyougou-content-service │
│  pinyougou-seckill-service      │                           │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑ MyBatis
┌─────────────────────────────────────────────────────────────┐
│               DAO 层 (Data Access Object)                     │
│         pinyougou-dao (Mapper 接口 + XML)                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   基础设施层 (Infrastructure)                  │
│  pinyougou-pojo  │  pinyougou-common  │  pinyougou-util    │
└─────────────────────────────────────────────────────────────┘
```

### 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| pinyougou-sellergoods-service | 9001 | 商家商品服务（商品增删改查） |
| pinyougou-manager-web | 9101 | 运营商后台管理系统 |
| pinyougou-content-service | 9002 | 内容广告服务 |
| pinyougou-portal-web | 9103 | 门户前台（广告展示） |
| pinyougou-user-service | 9003 | 用户服务 |
| pinyougou-user-web | 9104 | 用户中心 |
| pinyougou-order-service | 9004 | 订单服务 |
| pinyougou-cart-service | 9005 | 购物车服务 |
| pinyougou-search-service | 9006 | 商品搜索服务 |
| pinyougou-search-web | 9105 | 搜索页面 |
| pinyougou-page-service | 9007 | 商品详情页静态化服务 |
| pinyougou-pay-service | 9008 | 支付服务 |
| pinyougou-seckill-service | 9009 | 秒杀服务 |
| pinyougou-shop-web | 9106 | 商家管理后台 |

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6.3+
- MySQL 5.1+
- Redis 3.0+
- Solr 4.10+
- ActiveMQ 5.11+

### 数据库配置

1. 创建数据库 `pinyougou`
2. 导入 `Doc/pinyougou.sql` 数据库脚本
3. 修改各模块的 `application.properties` 配置数据库连接

### Redis 配置

修改各模块的 `redis-config.properties` 配置 Redis 连接：

```properties
redis.host=localhost
redis.port=6379
redis.password=
```

### Solr 配置

1. 安装 Solr 4.10+
2. 创建 core `pinyougou`
3. 修改 `solr-config.properties` 配置 Solr 地址

### ActiveMQ 配置

修改各模块的 `activemq.xml` 配置 ActiveMQ 连接：

```xml
<property name="brokerURL" value="tcp://localhost:61616" />
```

### 编译与运行

```bash
# 1. 克隆项目
git clone https://gitee.com/itxinfei/pinyougou-parent.git
cd pinyougou-parent

# 2. 编译项目（跳过测试）
mvn clean compile -DskipTests

# 3. 安装到本地仓库
mvn clean install -DskipTests

# 4. 启动服务（建议使用 IDEA 启动各个 Service 模块）
```

## 📁 项目结构

```
pinyougou-parent
├── pinyougou-pojo              # 通用实体类层
├── pinyougou-common            # 通用工具类、异常处理
├── pinyougou-dao               # 数据访问层（Mapper + XML）
├── pinyougou-interface         # 各服务接口定义
│   ├── pinyougou-cart-interface
│   ├── pinyougou-content-interface
│   ├── pinyougou-order-interface
│   ├── pinyougou-page-interface
│   ├── pinyougou-pay-interface
│   ├── pinyougou-search-interface
│   ├── pinyougou-seckill-interface
│   ├── pinyougou-sellergoods-interface
│   ├── pinyougou-user-interface
│   └── pinyougou-util-interface
├── pinyougou-service           # 各服务实现层
│   ├── pinyougou-sellergoods-service
│   ├── pinyougou-user-service
│   ├── pinyougou-order-service
│   ├── pinyougou-cart-service
│   ├── pinyougou-search-service
│   ├── pinyougou-page-service
│   ├── pinyougou-pay-service
│   ├── pinyougou-content-service
│   └── pinyougou-seckill-service
├── pinyougou-web               # 各 Web 层
│   ├── pinyougou-manager-web    # 运营商后台
│   ├── pinyougou-shop-web       # 商家后台
│   ├── pinyougou-user-web       # 用户中心
│   ├── pinyougou-cart-web       # 购物车
│   ├── pinyougou-search-web     # 搜索
│   ├── pinyougou-portal-web     # 门户
│   ├── pinyougou-pay-web        # 支付
│   └── pinyougou-seckill-web    # 秒杀
├── pinyougou-util              # 工具类（Solr 工具等）
└── Doc                         # 数据库脚本、讲义文档
```

## 🛠️ 核心功能

### 1. 商品管理
- 商品分类管理（三级分类）
- 品牌管理
- 规格参数管理
- 商品审核流程
- 商品上下架

### 2. 商品搜索
- 基于 Solr 全文检索
- 多条件组合查询
- 搜索结果高亮显示
- 分页查询

### 3. 购物车
- 商品添加/删除
- 数量修改
- 购物车金额实时计算
- 登录后合并购物车

### 4. 订单管理
- 订单创建
- 订单状态流转
- 订单查询
- 订单取消/删除

### 5. 支付功能
- 微信支付集成
- 支付回调处理
- 支付状态查询

### 6. 秒杀活动
- 秒杀商品管理
- 秒杀订单生成
- 防止超卖（Redis + 数据库双重验证）

### 7. 内容管理
- 广告分类管理
- 广告内容管理
- 首页轮播图
- 基于 Redis 缓存优化

### 8. 静态化页面
- 商品详情页静态化（Freemarker）
- 基于 ActiveMQ 异步生成
- 商品更新时自动删除并重新生成

## 💻 开发规范

### 代码规范

本项目遵循 **阿里巴巴Java开发手册** 规范：

- ✅ 统一使用 `entity.Result` 作为 Controller 返回对象
- ✅ 统一异常处理，避免空 catch 块
- ✅ 使用 Logger 替代 `e.printStackTrace()` 和 `System.out.println()`
- ✅ BigDecimal 计算使用 `multiply()` 等方法，避免 `new BigDecimal(double)`
- ✅ 数据库更新使用原子操作，防止并发问题
- ✅ 密码使用 BCrypt 加密存储

### Git 提交规范

本项目遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Type 类型：**
- `feat`: 新功能
- `fix`: 修复 bug
- `refactor`: 代码重构
- `docs`: 文档更新
- `test`: 测试相关
- `chore`: 构建/工具相关
- `perf`: 性能优化
- `ci`: CI 配置

## 📚 文档

详细的开发文档和部署文档请查看 [Doc](Doc/) 目录：

- [数据库设计](Doc/pinyougou.sql) - 完整的数据库表结构
- [部署讲义](Doc/部署讲义.md) - 环境部署指南
- [开发讲义](Doc/开发讲义.md) - 开发规范和技术要点

## 🔧 常见问题

### Q1: 启动时提示端口被占用
A: 检查各模块的端口配置，确保没有冲突，或修改 `application.properties` 中的端口配置。

### Q2: Redis 连接失败
A: 确保 Redis 服务已启动，并检查 `redis-config.properties` 配置是否正确。

### Q3: Solr 初始化失败
A: 确保 Solr 服务已启动，并且已创建 `pinyougou` core。

### Q4: ActiveMQ 连接失败
A: 确保 ActiveMQ 服务已启动，并检查 brokerURL 配置。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📝 更新日志

查看 [Releases](../../releases) 页面了解版本更新详情。

### 2026-07-01 - v1.1.0

**功能修复：**
- ✅ 实现 GoodsServiceImpl 所有 stub 方法
- ✅ 实现 GoodsController 空方法
- ✅ 消除 ItemSearchController 双重搜索调用
- ✅ 修复 BigDecimal 精度问题
- ✅ 修复库存扣减线程安全问题
- ✅ 添加 ItemPageServiceImpl null 检查
- ✅ 恢复 ContentServiceImpl @Transactional 注解

**代码质量：**
- ✅ 删除 Result 重复 flag 字段
- ✅ 统一异常处理（0 处 e.printStackTrace()）
- ✅ 全部替换 System.out.println 为 Logger（0 处遗留）
- ✅ 修复空 catch 块（0 处遗留）
- ✅ 统一异常类包路径

**安全加固：**
- ✅ 所有 spring-security.xml 使用 BCryptPasswordEncoder
- ✅ 移除硬编码密码

**代码规范：**
- ✅ 遵循阿里巴巴 Java 代码规范
- ✅ 修复编译器版本为 1.8

### 2026-06-30 - v1.0.0

- 🎉 项目初始版本
- ✅ 完成核心电商业务功能
- ✅ 完成分布式架构搭建

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 👨‍💻 作者

**心飞为你飞** - [GitHub](https://github.com/itxinfei) | [Gitee](https://gitee.com/itxinfei)

## 📞 联系方式

- 📧 Email: [747011882@qq.com](mailto:747011882@qq.com)
- 💬 QQ群: 863662849
- 🐛 问题反馈: [提交 Issue](https://gitee.com/itxinfei/pinyougou-parent/issues)

## ⭐ Star History

如果这个项目对你有帮助，请给个 Star ⭐ 支持一下！

---

<div align="center">
  <b>Made with ❤️ by <a href="https://gitee.com/itxinfei">心飞为你飞</a></b>
</div>
