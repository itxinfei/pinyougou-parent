#!/bin/bash

# 品优购项目基础设施检查脚本
# 用于检查所有必需的依赖服务是否正常运行

echo "========================================"
echo "  品优购项目 - 基础设施检查"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查计数
PASS=0
FAIL=0
WARN=0

# 检查函数
check_service() {
    local name=$1
    local cmd=$2
    local expected=$3

    echo -n "检查 $name ... "

    if eval "$cmd" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 通过${NC}"
        ((PASS++))
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        ((FAIL++))
        return 1
    fi
}

# 1. MySQL
echo "1️⃣  MySQL 数据库"
echo "----------------------------------------"
check_service "MySQL 端口 3306" "nc -zv 127.0.0.1 3306"

echo -n "检查 MySQL 数据库 ... "
if mysql -u root -proot -e "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 连接成功${NC}"
    ((PASS++))

    echo -n "检查数据库 pinyougoudb ... "
    if mysql -u root -proot -e "USE pinyougoudb; SELECT 1" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 数据库存在${NC}"
        ((PASS++))

        echo -n "检查表数量 ... "
        table_count=$(mysql -u root -proot -e "USE pinyougoudb; SHOW TABLES" 2>/dev/null | wc -l)
        # 减去标题行
        table_count=$((table_count - 1))
        echo "$table_count 张表"

        if [ $table_count -ge 23 ]; then
            echo -e "${GREEN}✓ 表数量正常${NC}"
            ((PASS++))
        else
            echo -e "${YELLOW}⚠ 表数量不足（期望 23 张，实际 $table_count 张）${NC}"
            ((WARN++))
        fi
    else
        echo -e "${RED}✗ 数据库不存在${NC}"
        ((FAIL++))
    fi
else
    echo -e "${RED}✗ MySQL 未启动或连接失败${NC}"
    ((FAIL+=2))
fi
echo ""

# 2. Redis
echo "2️⃣  Redis 缓存"
echo "----------------------------------------"
check_service "Redis 端口 6379" "nc -zv 127.0.0.1 6379"

if command -v redis-cli &> /dev/null; then
    check_service "Redis PING" "redis-cli ping"
else
    echo -e "${YELLOW}⚠ redis-cli 未安装，跳过 PING 检查${NC}"
    ((WARN++))
fi
echo ""

# 3. Zookeeper
echo "3️⃣  Zookeeper"
echo "----------------------------------------"
echo -n "检查 Zookeeper ... "

# 从 dubbox_dev.properties 读取地址
ZK_ADDR="172.168.20.221:2181"
if [ -f "pinyougou-common/src/main/resources/filters/dubbox_dev.properties" ]; then
    ZK_ADDR=$(grep "env.address" pinyougou-common/src/main/resources/filters/dubbox_dev.properties | cut -d'=' -f2)
fi

ZK_HOST=$(echo $ZK_ADDR | cut -d':' -f1)
ZK_PORT=$(echo $ZK_ADDR | cut -d':' -f2)

if nc -zv $ZK_HOST $ZK_PORT > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Zookeeper 可连接 ($ZK_ADDR)${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ Zookeeper 无法连接 ($ZK_ADDR)${NC}"
    ((FAIL++))
fi
echo ""

# 4. Solr
echo "4️⃣  Solr 搜索引擎"
echo "----------------------------------------"
check_service "Solr 端口 8983" "nc -zv 127.0.0.1 8983" || true

if command -v curl &> /dev/null; then
    echo -n "检查 Solr Core ... "
    response=$(curl -s "http://localhost:8983/solr/admin/cores?action=LIST" 2>/dev/null)
    if echo "$response" | grep -q "pinyougou"; then
        echo -e "${GREEN}✓ Core pinyougou 存在${NC}"
        ((PASS++))
    else
        echo -e "${YELLOW}⚠ Core pinyougou 未找到${NC}"
        ((WARN++))
    fi
fi
echo ""

# 5. ActiveMQ
echo "5️⃣  ActiveMQ 消息队列"
echo "----------------------------------------"
check_service "ActiveMQ 端口 61616" "nc -zv 127.0.0.1 61616" || true
check_service "ActiveMQ 管理后台 8161" "nc -zv 127.0.0.1 8161" || true
echo ""

# 总结
echo "========================================"
echo "  检查总结"
echo "========================================"
echo -e "${GREEN}✓ 通过: $PASS${NC}"
echo -e "${RED}✗ 失败: $FAIL${NC}"
echo -e "${YELLOW}⚠ 警告: $WARN${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ 所有基础设施检查通过！${NC}"
    echo "可以开始测试服务接口..."
    exit 0
else
    echo -e "${RED}✗ 部分基础设施检查失败！${NC}"
    echo "请先启动失败的服务后再进行测试。"
    exit 1
fi
