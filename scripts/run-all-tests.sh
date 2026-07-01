#!/bin/bash

# 品优购项目完整测试脚本
# 一键运行所有测试

echo "========================================"
echo "  品优购项目 - 完整测试套件"
echo "========================================"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 阶段 1: 基础设施检查
echo "📍 阶段 1/4: 基础设施检查"
echo "========================================"
bash "$SCRIPT_DIR/test-infrastructure.sh"
if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ 基础设施检查失败，请先启动基础设施服务${NC}"
    exit 1
fi
echo ""

# 阶段 2: Service 层测试
echo "📍 阶段 2/4: Service 层接口测试"
echo "========================================"
bash "$SCRIPT_DIR/test-service-apis.sh"
if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ Service 层接口测试失败${NC}"
    exit 1
fi
echo ""

# 阶段 3: Web 层测试
echo "📍 阶段 3/4: Web 层接口测试"
echo "========================================"
bash "$SCRIPT_DIR/test-web-apis.sh"
if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ Web 层接口测试失败${NC}"
    exit 1
fi
echo ""

# 阶段 4: 前端页面测试
echo "📍 阶段 4/4: 前端页面测试"
echo "========================================"
bash "$SCRIPT_DIR/test-web-pages.sh"
if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ 前端页面测试失败${NC}"
    exit 1
fi
echo ""

# 生成测试报告
echo "========================================"
echo "  生成测试报告"
echo "========================================"
bash "$SCRIPT_DIR/generate-test-report.sh"

echo ""
echo -e "${GREEN}✓ 所有测试完成！${NC}"
echo "详细报告请查看: docs/接口测试报告-$(date +%Y-%m-%d).md"
