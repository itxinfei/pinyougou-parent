# 📚 品优购项目文档中心

欢迎来到品优购项目文档中心！这里包含了项目的所有重要文档和学习资料。

---

## 📖 必读文档

### 🚀 入门指南

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| [../README.md](../README.md) | 项目总览、快速开始 | 所有人 |
| [环境搭建指南.md](环境搭建指南.md) | 详细的环境配置步骤 | 初次接触项目的开发者 |
| [数据库设计说明.md](数据库设计说明.md) | 数据库表结构详解 | 需要理解数据模型的开发者 |

### 💻 开发文档

| 文档 | 说明 | 阅读时机 |
|------|------|---------|
| [开发规范.md](开发规范.md) | 代码规范、Git 提交规范 | 开始编码前必读 |
| [API 接口文档.md](API接口文档.md) | 所有 RESTful API 接口 | 前后端联调时 |
| [架构设计.md](架构设计.md) | 系统架构详解 | 深入理解项目时 |

### 🔧 部署文档

| 文档 | 说明 |
|------|------|
| [部署指南.md](部署指南.md) | 生产环境部署步骤 |
| [配置文件说明.md](配置文件说明.md) | 所有配置项详解 |

### 📊 专项文档

| 文档 | 说明 |
|------|------|
| [数据库脚本.sql](../pinyougoudb-v1.3.sql) | 完整数据库脚本（23 张表） |
| [项目审查报告-2026-07-01.md](../project-review-report-2026-07-01.md) | 代码质量审查报告 |

---

## 🗺️ 学习路线图

```
第一阶段：环境搭建
├─ 1. 安装 JDK 1.8
├─ 2. 安装 Maven 3.6.3
├─ 3. 安装 MySQL 5.7
├─ 4. 安装 Redis 3.0
├─ 5. 安装 Solr 4.10
└─ 6. 安装 ActiveMQ 5.11

第二阶段：项目导入
├─ 1. 克隆项目
├─ 2. 导入数据库
├─ 3. 配置环境
├─ 4. 编译项目
└─ 5. 启动服务

第三阶段：源码学习
├─ 1. 理解项目结构
├─ 2. 学习基础模块（pojo、common、dao）
├─ 3. 学习服务层（从商品服务开始）
├─ 4. 学习 Web 层
└─ 5. 整合练习

第四阶段：进阶提升
├─ 1. 分布式事务
├─ 2. 高并发场景
├─ 3. 性能优化
└─ 4. 二次开发
```

---

## 📝 文档编写规范

### 文档格式

- 使用 Markdown 格式（`.md`）
- 中文文档UTF-8编码
- 适当使用表情符号提升可读性
- 代码块指定语言类型

### 文档分类

- `指南-*.md` - 操作指南类文档
- `设计-*.md` - 设计说明类文档
- `规范-*.md` - 规范要求类文档
- `报告-*.md` - 报告总结类文档

---

## 🔍 快速查找

### 按技术栈查找

- **Spring**: 所有 Service 模块
- **Dubbo**: interface 和 service 模块
- **MyBatis**: dao 模块
- **Redis**: cart-service、order-service、search-service
- **Solr**: search-service
- **ActiveMQ**: page-service、order-service
- **JWT**: user-service、user-web

### 按功能模块查找

- **用户相关**: user-interface、user-service、user-web
- **商品相关**: sellergoods-interface、sellergoods-service
- **订单相关**: order-interface、order-service
- **购物车**: cart-interface、cart-service、cart-web
- **支付**: pay-interface、pay-service
- **搜索**: search-interface、search-service、search-web

---

## 📞 需要帮助？

- 📧 邮箱：[747011882@qq.com](mailto:747011882@qq.com)
- 💬 QQ 群：863662849
- 🐛 问题反馈：[提交 Issue](https://gitee.com/itxinfei/pinyougou-parent/issues)

---

<div align="center">
  <p>📖 <a href="../README.md">返回项目首页</a></p>
  <p>Made with ❤️ by <a href="https://gitee.com/itxinfei">心飞为你飞</a></p>
</div>
