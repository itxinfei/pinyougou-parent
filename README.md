# 🛍️ 品优购电商平台 (Pinyougou E-commerce Platform)

[![JDK 1.8+](https://img.shields.io/badge/JDK-1.8+-brightgreen.svg)]()
[![Maven 3.6.3+](https://img.shields.io/badge/Maven-3.6.3+-yellowgreen.svg)]()
[![License Apache](https://img.shields.io/badge/License-Apache-green.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-blue.svg)]()
[![Dubbo](https://img.shields.io/badge/Dubbo-2.8.4-orange.svg)]()

> 一个基于分布式架构的B2C电商平台，采用微服务架构设计，提供完整的电商解决方案。

## 📖 项目简介

品优购电商平台是一个完整的B2C电子商务解决方案，采用前后端分离架构，支持多商家入驻、商品管理、订单处理、支付集成、搜索推荐等核心电商功能。

### ✨ 核心特性

- **分布式架构**：基于Dubbo + ZooKeeper的微服务架构
- **前后端分离**：RESTful API接口，支持多端接入
- **高可用设计**：服务注册发现、负载均衡、熔断降级
- **大数据支持**：集成Solr搜索、Redis缓存、消息队列
- **安全可靠**：Spring Security安全框架，CAS单点登录
- **扩展性强**：模块化设计，支持快速扩展新功能

## 🏗️ 系统架构

### 技术栈

| 技术领域 | 技术选型 |
|---------|---------|
| **后端框架** | Spring 4.2.5.RELEASE, Spring MVC, MyBatis 3.2.8 |
| **分布式框架** | Dubbo 2.8.4, ZooKeeper 3.4.6 |
| **数据库** | MySQL 5.1.32, Redis 2.8.1, Solr 4.10.3 |
| **消息队列** | ActiveMQ 5.11.2 |
| **安全框架** | Spring Security 3.2.3.RELEASE, CAS 3.3.3 |
| **前端技术** | Freemarker 2.3.23, jQuery, Bootstrap |
| **构建工具** | Maven 3.6.3+, JDK 1.8+ |
| **其他组件** | FastDFS 1.2（文件存储）, Druid（连接池）, PageHelper（分页） |

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                       用户界面层                              │
│  (Portal Web, Manager Web, Shop Web, Mobile App)           │
└─────────────────┬─────────────────┬─────────────────────────┘
                  │ HTTP/REST       │
┌─────────────────▼─────────────────▼─────────────────────────┐
│                      Web服务层                                │
│  (Cart Web, Search Web, User Web, Seckill Web, etc.)       │
└─────────────────┬─────────────────┬─────────────────────────┘
                  │ Dubbo RPC       │
┌─────────────────▼─────────────────▼─────────────────────────┐
│                     业务服务层                                │
│  (Cart Service, Order Service, User Service, etc.)         │
└─────────────────┬─────────────────┬─────────────────────────┘
                  │ DAO             │
┌─────────────────▼─────────────────▼─────────────────────────┐
│                     数据访问层                                │
│  (MyBatis, Redis, Solr, MySQL, ActiveMQ)                   │
└─────────────────────────────────────────────────────────────┘
```

## 📦 项目模块结构

```
pinyougou-parent/                    # 父项目（聚合工程）
├── pinyougou-common/                # 通用工具模块
├── pinyougou-dao/                   # 数据访问层
├── pinyougou-pojo/                  # 实体类模块
├── pinyougou-solr-util/             # Solr搜索工具
├── pinyougou-manager-web/           # 后台管理系统
├── pinyougou-portal-web/            # 门户网站
├── pinyougou-shop-web/              # 商家后台
├── pinyougou-search-web/            # 搜索服务Web层
├── pinyougou-search-service/        # 搜索业务服务
├── pinyougou-search-interface/      # 搜索接口定义
├── pinyougou-cart-web/              # 购物车Web层
├── pinyougou-cart-service/          # 购物车业务服务
├── pinyougou-cart-interface/        # 购物车接口定义
├── pinyougou-order-web/             # 订单Web层
├── pinyougou-order-service/         # 订单业务服务
├── pinyougou-order-interface/       # 订单接口定义
├── pinyougou-user-web/              # 用户Web层
├── pinyougou-user-service/          # 用户业务服务
├── pinyougou-user-interface/        # 用户接口定义
├── pinyougou-pay-web/               # 支付Web层
├── pinyougou-pay-service/           # 支付业务服务
├── pinyougou-pay-interface/         # 支付接口定义
├── pinyougou-seckill-web/           # 秒杀Web层
├── pinyougou-seckill-service/       # 秒杀业务服务
├── pinyougou-seckill-interface/     # 秒杀接口定义
├── pinyougou-content-web/           # 内容管理Web层
├── pinyougou-content-service/       # 内容业务服务
├── pinyougou-content-interface/     # 内容接口定义
├── pinyougou-sellergoods-web/       # 商家商品Web层
├── pinyougou-sellergoods-service/   # 商家商品服务
├── pinyougou-sellergoods-interface/ # 商家商品接口
├── pinyougou-page-web/              # 页面生成Web层
├── pinyougou-page-service/          # 页面生成服务
├── pinyougou-page-interface/        # 页面生成接口
└── docs/                            # 项目文档
```

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6.3+
- MySQL 5.7+
- Redis 3.2+
- ZooKeeper 3.4+
- ActiveMQ 5.11+
- Solr 4.10+

### 安装步骤

1. **克隆项目**
  
```bash
   git clone https://gitee.com/itxinfei/pinyougou-parent.git
   cd pinyougou-parent
   ```

2. **导入数据库**
   - 创建数据库 `pinyougou_db`
   - 执行项目中的SQL脚本（位于各模块的resources目录）

3. **配置环境**
   - 修改各模块的 `application.properties` 或 `application.yml`
   - 配置数据库连接、Redis、ZooKeeper等中间件

4. **启动服务**
  
```bash
   # 1. 启动ZooKeeper
   zkServer start
   
   # 2. 启动ActiveMQ
   activemq start
   
   # 3. 启动Solr
   solr start
   
   # 4. 编译项目
   mvn clean install -DskipTests
   
   # 5. 按顺序启动服务（建议使用IDEA运行）
   # 启动顺序：common → dao → service → web
   ```

5. **访问系统**
   - 后台管理：http://localhost:8080/manage
   - 门户网站：http://localhost:8081
   - 商家后台：http://localhost:8082

## 📚 功能模块

### 核心功能

| 模块 | 功能描述 |
|------|---------|
| **商品管理** | 商品分类、品牌管理、规格参数、商品上下架 |
| **订单管理** | 订单创建、支付、发货、退款、售后 |
| **用户中心** | 用户注册、登录、个人信息、收货地址 |
| **购物车** | 商品加入购物车、批量操作、优惠计算 |
| **搜索服务** | 全文搜索、商品筛选、排序、推荐 |
| **秒杀系统** | 限时抢购、库存控制、防超卖 |
| **支付系统** | 多种支付方式、支付回调、对账 |
| **内容管理** | 广告管理、文章发布、页面配置 |
| **权限管理** | 角色权限、菜单管理、操作日志 |

### 技术亮点

1. **分布式事务**：使用消息队列实现最终一致性
2. **缓存策略**：多级缓存（Redis + 本地缓存）
3. **搜索优化**：Solr分词、高亮、聚合查询
4. **安全防护**：XSS过滤、SQL注入防护、CSRF防护
5. **性能优化**：数据库分库分表、读写分离、CDN加速

## 🔧 开发指南

### 代码规范

- 遵循阿里巴巴Java开发规范
- 使用Lombok减少样板代码
- 统一异常处理机制
- 日志使用SLF4J + Logback

### API设计

- RESTful风格接口
- 统一响应格式
- 接口版本管理
- Swagger API文档

### 测试策略

- 单元测试：JUnit + Mockito
- 集成测试：Spring Test
- 性能测试：JMeter
- 接口测试：Postman

## 🤝 贡献指南

我们欢迎任何形式的贡献！

1. **报告问题**：在Issues中提交bug报告或功能建议
2. **提交代码**：Fork项目，创建功能分支，提交Pull Request
3. **完善文档**：帮助改进文档和示例代码
4. **分享经验**：在讨论区分享使用经验

### 开发流程


```bash
# 1. Fork项目
# 2. 克隆你的fork
git clone https://gitee.com/your-username/pinyougou-parent.git

# 3. 创建功能分支
git checkout -b feature/your-feature

# 4. 提交更改
git add .
git commit -m "feat: add your feature"

# 5. 推送到远程
git push origin feature/your-feature

# 6. 创建Pull Request
```

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

## 📞 联系方式

- **项目作者**：itxinfei
- **GitHub仓库**：https://gitee.com/itxinfei/pinyougou-parent
- **问题反馈**：[Issues](https://gitee.com/itxinfei/pinyougou-parent/issues)
- **QQ交流群**：661543188
- **邮箱**：74011882@qq.com

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者！

---

**⭐ 如果这个项目对你有帮助，请点个Star支持一下！**
```
