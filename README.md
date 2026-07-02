<p align="center">
  <img src="docs/Logo.png" alt="品优购" width="400">
</p>

<h1 align="center">品优购网上商城</h1>

<p align="center">
  <strong>基于 Spring + Dubbo 微服务架构的 B2B2C 电商平台</strong>
</p>

<p align="center">
  <a href="https://gitee.com/itxinfei/pinyougou-parent/stargazers">
    <img src="https://gitee.com/itxinfei/pinyougou-parent/badge/star.svg?theme=dark" alt="star">
  </a>
  <a href="https://gitee.com/itxinfei/pinyougou-parent/members">
    <img src="https://gitee.com/itxinfei/pinyougou-parent/badge/fork.svg?theme=dark" alt="fork">
  </a>
  <img src="https://img.shields.io/badge/JDK-1.8+-green?style=flat&logo=openjdk" alt="JDK">
  <img src="https://img.shields.io/badge/Spring-5.3-6db33f?style=flat&logo=spring" alt="Spring">
  <img src="https://img.shields.io/badge/Dubbo-2.8-blue?style=flat" alt="Dubbo">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="license">
</p>

---

## 项目简介

品优购网上商城是一个完整的 B2B2C 电商平台 Demo，采用**微服务架构**设计，涵盖从商品管理到订单支付的全流程电商业务场景。适合 Java 后端开发者学习微服务架构、分布式系统设计。

### 适合人群

- Java 后端开发初学者
- 想学习微服务架构的开发者
- 需要电商项目实战经验的人
- 计算机相关专业的学生

---

## 核心特性

<table>
  <tr>
    <td width="50%">
      <h4>商品管理</h4>
      <p>分类、品牌、规格、模板完整 CRUD</p>
    </td>
    <td width="50%">
      <h4>商品搜索</h4>
      <p>Solr 全文检索、高亮显示、分词器</p>
    </td>
  </tr>
  <tr>
    <td>
      <h4>购物车</h4>
      <p>Redis 存储、分布式锁、购物车合并</p>
    </td>
    <td>
      <h4>订单管理</h4>
      <p>状态流转、订单取消、退款申请</p>
    </td>
  </tr>
  <tr>
    <td>
      <h4>支付集成</h4>
      <p>微信支付、回调处理、支付日志</p>
    </td>
    <td>
      <h4>秒杀活动</h4>
      <p>Redis + Lua 原子操作、防止超卖</p>
    </td>
  </tr>
  <tr>
    <td>
      <h4>安全认证</h4>
      <p>JWT Token、BCrypt加密、登录限流</p>
    </td>
    <td>
      <h4>页面静态化</h4>
      <p>Freemarker + ActiveMQ 异步处理</p>
    </td>
  </tr>
</table>

---

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Web 层 (Presentation)                      │
│   运营商后台 │ 商家后台 │ 用户中心 │ 购物车 │ 搜索 │ 门户    │
└───────────────────────────┬─────────────────────────────────┘
                            │ Dubbo RPC
┌───────────────────────────┴─────────────────────────────────┐
│                  Service 层 (Business Logic)                  │
│   商品服务 │ 用户服务 │ 订单服务 │ 购物车 │ 搜索 │ 支付等   │
└───────────────────────────┬─────────────────────────────────┘
                            │ MyBatis
┌───────────────────────────┴─────────────────────────────────┐
│               DAO 层 (Data Access Object)                     │
│            Mapper 接口 + XML 映射文件                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────┐
│            基础设施层 (MySQL + Redis + Solr + MQ)             │
└─────────────────────────────────────────────────────────────┘
```

---

## 技术栈

| 技术 | 版本 | 用途 | 官网 |
|------|------|------|------|
| Spring | 5.3.31 | IoC/AOP/事务管理 | [spring.io](https://spring.io) |
| Dubbo | 2.8.4 | RPC远程调用 | [dubbo.apache.org](https://dubbo.apache.org) |
| MyBatis | 3.2.8 | ORM持久层 | [mybatis.org](https://mybatis.org) |
| MySQL | 5.7+ | 关系数据库 | [mysql.com](https://www.mysql.com) |
| Redis | 3.0+ | 缓存/分布式锁 | [redis.io](https://redis.io) |
| Solr | 4.10+ | 全文检索 | [solr.apache.org](https://solr.apache.org) |
| ActiveMQ | 5.11+ | 消息队列 | [activemq.apache.org](https://activemq.apache.org) |
| Spring Security | 5.7.11 | 认证授权 | [spring.io/projects/spring-security](https://spring.io/projects/spring-security) |
| JJWT | 0.9.0 | Token认证 | [github.com/jwtk/jjwt](https://github.com/jwtk/jjwt) |

---

## 项目结构

```
pinyougou-parent/
│
├── pinyougou-pojo/              # 实体类层（23个表）
├── pinyougou-common/             # 公共工具层
│   ├── util/                     # 工具类（JWT、ID生成等）
│   └── exception/                # 自定义异常
├── pinyougou-dao/                # 数据访问层（24个Mapper）
│
├── pinyougou-*-interface/        # 服务接口层（9个模块）
├── pinyougou-*-service/          # 服务实现层（9个模块）
│
└── pinyougou-*-web/              # Web层（8个模块）
    ├── pinyougou-manager-web     # 运营商后台（9101）
    ├── pinyougou-shop-web        # 商家后台（9106）
    ├── pinyougou-user-web        # 用户中心（9104）
    ├── pinyougou-cart-web        # 购物车服务
    ├── pinyougou-search-web      # 搜索服务
    └── pinyougou-portal-web      # 门户前台
```

---

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| sellergoods-service | 9001 | 商品服务 |
| content-service | 9002 | 广告服务 |
| user-service | 9003 | 用户服务 |
| order-service | 9004 | 订单服务 |
| cart-service | 9005 | 购物车服务 |
| search-service | 9006 | 搜索服务 |
| page-service | 9007 | 页面服务 |
| pay-service | 9008 | 支付服务 |
| seckill-service | 9009 | 秒杀服务 |
| manager-web | 9101 | 运营后台 |
| portal-web | 9103 | 门户前台 |
| user-web | 9104 | 用户中心 |
| search-web | 9105 | 搜索页面 |
| shop-web | 9106 | 商家后台 |

---

## 快速开始

### 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8+ | Java 开发工具包 |
| Maven | 3.6.3+ | 项目构建工具 |
| MySQL | 5.7+ | 关系数据库 |
| Redis | 3.0+ | 缓存服务 |
| Solr | 4.10+ | 搜索引擎 |
| ActiveMQ | 5.11+ | 消息队列 |

### 安装步骤

**1. 克隆项目**

```bash
git clone https://gitee.com/itxinfei/pinyougou-parent.git
cd pinyougou-parent
```

**2. 导入数据库**

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE pinyougoudb DEFAULT CHARACTER SET utf8"

# 导入数据
mysql -u root -p pinyougoudb < docs/pinyougoudb-v1.3.sql
```

**3. 编译项目**

```bash
mvn clean install -DskipTests
```

**4. 启动服务**

```
启动顺序：
1. 基础设施：Redis → MySQL → Solr → ActiveMQ
2. Service层：商品 → 用户 → 订单 → 购物车 → 搜索 → 内容 → 页面 → 支付 → 秒杀
3. Web层：运营后台 → 商家后台 → 用户中心 → 购物车 → 搜索 → 门户
```

---

## 文档

| 文档 | 说明 |
|------|------|
| [环境搭建指南](docs/环境搭建指南.md) | 详细的环境配置步骤 |
| [数据库设计说明](docs/数据库设计说明.md) | 23张表结构详解 |
| [开发规范](docs/开发规范.md) | 代码规范、Git提交规范 |

---

## 更新日志

### v1.2.0 (2026-07-02)

- Spring 版本升级 4.2.5 → 5.3.31
- 修复注册密码 MD5 → BCrypt 加密
- 修复秒杀超卖问题（Lua 原子操作）
- 修复退款事务失效问题
- 修复订单 IDOR 安全漏洞
- 修复支付日志金额计算错误
- 优化 README 文档

### v1.1.0 (2026-07-01)

- 新增订单取消/退款功能
- 新增 Token 黑名单机制
- 新增登录失败限制

### v1.0.0 (2026-06-30)

- 项目初始版本发布

---

## 贡献指南

欢迎 Fork 和 Star 本项目！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 作者

**心飞为你飞**

- Gitee: [@itxinfei](https://gitee.com/itxinfei)
- Email: 747011882@qq.com

---

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证

---

<p align="center">
  如果这个项目对你有帮助，请给个 <b>Star</b> 支持一下！
</p>
