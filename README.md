# 品优购网上商城

基于 Spring + Dubbo 的 B2B2C 电商平台，涵盖商品管理、购物车、订单、支付、秒杀等完整电商业务流程。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring | 5.3.31 | IoC/AOP/事务 |
| Dubbo | 2.8.4 | RPC远程调用 |
| MyBatis | 3.2.8 | ORM持久层 |
| MySQL | 5.7+ | 关系数据库 |
| Redis | 3.0+ | 缓存/分布式锁 |
| Solr | 4.10+ | 全文检索 |
| ActiveMQ | 5.11+ | 消息队列 |
| Spring Security | 5.7.11 | 认证授权 |
| JJWT | 0.9.0 | Token认证 |

## 项目结构

```
pinyougou-parent/
├── pinyougou-pojo/          # 实体类（23个表）
├── pinyougou-common/         # 工具类（JWT、ID生成等）
├── pinyougou-dao/            # MyBatis Mapper
├── pinyougou-*-interface/    # 服务接口（9个）
├── pinyougou-*-service/      # 服务实现（9个）
└── pinyougou-*-web/          # Web层（8个）
```

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

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6.3+
- MySQL 5.7+
- Redis 3.0+
- Solr 4.10+
- ActiveMQ 5.11+

### 安装步骤

```bash
# 1. 克隆项目
git clone https://gitee.com/itxinfei/pinyougou-parent.git
cd pinyougou-parent

# 2. 导入数据库
mysql -u root -p pinyougoudb < docs/pinyougoudb-v1.3.sql

# 3. 编译项目
mvn clean install -DskipTests

# 4. 启动服务（按顺序）
# 先启动：Redis → MySQL → Solr → ActiveMQ
# 再启动：Service层 → Web层
```

### 启动顺序

1. 基础设施：Redis、MySQL、Solr、ActiveMQ
2. Service层：商品→用户→订单→购物车→搜索→内容→页面→支付→秒杀
3. Web层：运营后台、商家后台、用户中心、购物车、搜索、门户

## 核心功能

- **商品管理**：分类、品牌、规格、模板 CRUD
- **商品搜索**：Solr 全文检索、高亮显示
- **购物车**：Redis 存储、分布式锁、购物车合并
- **订单管理**：状态流转、取消、退款
- **支付集成**：微信支付、回调处理
- **秒杀活动**：Redis + Lua 原子操作、防止超卖
- **安全认证**：JWT Token、BCrypt密码加密、登录限流

## 文档

| 文档 | 说明 |
|------|------|
| [环境搭建指南](docs/环境搭建指南.md) | 详细环境配置 |
| [数据库设计说明](docs/数据库设计说明.md) | 23张表结构 |
| [开发规范](docs/开发规范.md) | 代码规范 |

## 作者

**心飞为你飞**

- Gitee: [@itxinfei](https://gitee.com/itxinfei)
- Email: 747011882@qq.com

## 许可证

Apache License 2.0
