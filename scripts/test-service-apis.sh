#!/bin/bash

# 品优购项目 Service 层接口测试脚本
# 测试所有 Service 模块的核心接口

echo "========================================"
echo "  品优购项目 - Service 层接口测试"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0
SKIP=0

# 测试函数
test_service() {
    local name=$1
    local url=$2
    local expected=$3

    echo -n "测试 $name ... "

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)

    if [ "$response" = "$expected" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $response)"
        ((PASS++))
        return 0
    else
        echo -e "${RED}✗ 失败${NC} (HTTP $response, 期望 $expected)"
        ((FAIL++))
        return 1
    fi
}

# 1. 商品服务 (9001)
echo "1️⃣  商品服务 (pinyougou-sellergoods-service:9001)"
echo "----------------------------------------"

test_service "查询所有商品" \
    "http://localhost:9001/goods/findAll" \
    "200"

test_service "分页查询商品" \
    "http://localhost:9001/goods/findPage?pageNum=1&pageSize=10" \
    "200"

# 使用已知的商品ID 19 进行测试
test_service "查询单个商品 (ID:19)" \
    "http://localhost:9001/goods/findOne/19" \
    "200"

echo ""

# 2. 用户服务 (9003)
echo "2️⃣  用户服务 (pinyougou-user-service:9003)"
echo "----------------------------------------"

test_service "用户名登录" \
    "http://localhost:9003/login/loginByUsername?username=admin&password=123123" \
    "200"

test_service "短信登录" \
    "http://localhost:9003/login/loginBySms?phone=13900112222&code=123456" \
    "200"

echo ""

# 3. 订单服务 (9004)
echo "3️⃣  订单服务 (pinyougou-order-service:9004)"
echo "----------------------------------------"

test_service "查询所有订单" \
    "http://localhost:9004/order/findAll" \
    "200"

test_service "分页查询订单" \
    "http://localhost:9004/order/findPage?page=1&rows=10" \
    "200"

echo ""

# 4. 购物车服务 (9005)
echo "4️⃣  购物车服务 (pinyougou-cart-service:9005)"
echo "----------------------------------------"

test_service "查询购物车列表" \
    "http://localhost:9005/cart/findCartList" \
    "200"

echo ""

# 5. 搜索服务 (9006)
echo "5️⃣  搜索服务 (pinyougou-search-service:9006)"
echo "----------------------------------------"

test_service "搜索商品" \
    "http://localhost:9006/item/search" \
    "200"

echo ""

# 6. 内容服务 (9002)
echo "6️⃣  内容服务 (pinyougou-content-service:9002)"
echo "----------------------------------------"

test_service "查询广告分类" \
    "http://localhost:9002/contentCategory/findAll" \
    "200"

test_service "分页查询广告" \
    "http://localhost:9002/content/findPage?pageNum=1&pageSize=10" \
    "200"

echo ""

# 7. 支付服务 (9008)
echo "7️⃣  支付服务 (pinyougou-pay-service:9008)"
echo "----------------------------------------"

# 支付接口需要特定的参数，先跳过或使用 mock 数据
echo -e "${YELLOW}⚠ 支付接口需要真实订单数据，跳过测试${NC}"
((SKIP++))

echo ""

# 8. 秒杀服务 (9009)
echo "8️⃣  秒杀服务 (pinyougou-seckill-service:9009)"
echo "----------------------------------------"

test_service "查询秒杀商品" \
    "http://localhost:9009/seckillGoods/findAll" \
    "200"

echo ""

# 总结
echo "========================================"
echo "  测试总结"
echo "========================================"
echo -e "${GREEN}✓ 通过: $PASS${NC}"
echo -e "${RED}✗ 失败: $FAIL${NC}"
echo -e "${YELLOW}⚠ 跳过: $SKIP${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ Service 层接口测试通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 部分接口测试失败${NC}"
    exit 1
fi
