-- =====================================================
-- H2数据库Schema（简化版，用于集成测试）
-- 基于pinyougou的MySQL表结构转换
-- =====================================================

-- ========== 订单表 ==========
CREATE TABLE IF NOT EXISTS tb_order (
    order_id BIGINT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    payment_type VARCHAR(2) NOT NULL COMMENT '支付方式：1在线支付，2货到付款',
    post_fee DECIMAL(10,2) DEFAULT 0.00 COMMENT '邮费',
    status VARCHAR(10) NOT NULL COMMENT '订单状态',
    receiver VARCHAR(100) NOT NULL COMMENT '收货人',
    receiver_mobile VARCHAR(20) NOT NULL COMMENT '收货人电话',
    receiver_area_name VARCHAR(100) NOT NULL COMMENT '收货地址区域',
    address VARCHAR(500) COMMENT '详细地址',
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    payment DECIMAL(10,2) COMMENT '支付金额',
    pay_time TIMESTAMP COMMENT '支付时间',
    consign_time TIMESTAMP COMMENT '发货时间',
    end_time TIMESTAMP COMMENT '交易完成时间',
    close_time TIMESTAMP COMMENT '交易关闭时间',
    shipping_name VARCHAR(100) COMMENT '物流名称',
    shipping_code VARCHAR(100) COMMENT '物流单号',
    user_feedback VARCHAR(500) COMMENT '用户留言',
    comment_time TIMESTAMP COMMENT '评价时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ========== 订单明细表 ==========
CREATE TABLE IF NOT EXISTS tb_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL COMMENT '商品ID',
    goods_id BIGINT NOT NULL COMMENT 'SPU ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    title VARCHAR(500) NOT NULL COMMENT '商品标题',
    price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    num INT NOT NULL COMMENT '购买数量',
    total_fee DECIMAL(10,2) NOT NULL COMMENT '总价',
    pic_path VARCHAR(500) COMMENT '商品图片',
    seller_id VARCHAR(100) NOT NULL COMMENT '商家ID',
    INDEX idx_order_id (order_id),
    INDEX idx_item_id (item_id),
    INDEX idx_seller_id (seller_id),
    FOREIGN KEY (order_id) REFERENCES tb_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ========== 支付日志表 ==========
CREATE TABLE IF NOT EXISTS tb_pay_log (
    out_trade_no VARCHAR(100) PRIMARY KEY COMMENT '交易流水号',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    pay_type VARCHAR(2) NOT NULL COMMENT '支付类型：1微信，2支付宝',
    total_fee BIGINT NOT NULL COMMENT '支付金额（分）',
    trade_state VARCHAR(2) NOT NULL COMMENT '交易状态：0未支付，1已支付',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    pay_time TIMESTAMP COMMENT '支付时间',
    transaction_id VARCHAR(100) COMMENT '第三方交易流水号',
    order_list VARCHAR(500) COMMENT '订单ID列表（逗号分隔）',
    INDEX idx_user_id (user_id),
    INDEX idx_trade_state (trade_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ========== 用户表 ==========
CREATE TABLE IF NOT EXISTS tb_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    nick_name VARCHAR(100) COMMENT '昵称',
    status VARCHAR(2) NOT NULL DEFAULT '1' COMMENT '状态：1正常，0禁用',
    created TIMESTAMP NOT NULL COMMENT '创建时间',
    updated TIMESTAMP NOT NULL COMMENT '更新时间',
    source_type VARCHAR(2) NOT NULL DEFAULT '1' COMMENT '注册来源：1PC，2APP，3小程序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ========== 商品表（简化版） ==========
CREATE TABLE IF NOT EXISTS tb_item (
    id BIGINT PRIMARY KEY,
    goods_id BIGINT NOT NULL COMMENT 'SPU ID',
    title VARCHAR(500) NOT NULL COMMENT '商品标题',
    sell_point VARCHAR(1000) COMMENT '卖点',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    num INT NOT NULL COMMENT '库存数量',
    barcode VARCHAR(100) COMMENT '条形码',
    image VARCHAR(500) COMMENT '商品图片',
    category VARCHAR(100) COMMENT '分类',
    brand VARCHAR(100) COMMENT '品牌',
    seller VARCHAR(100) COMMENT '商家',
    status VARCHAR(2) NOT NULL COMMENT '状态：1正常，0下架，2删除',
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    INDEX idx_goods_id (goods_id),
    INDEX idx_category (category),
    INDEX idx_brand (brand),
    INDEX idx_seller (seller)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ========== 插入测试数据 ==========
-- 注意：仅用于集成测试，不包含敏感数据

-- 插入测试商品
INSERT INTO tb_item (id, goods_id, title, price, num, category, brand, seller, status, created, updated)
VALUES
(1001, 2001, '华为P50 Pro', 4999.00, 100, '手机', '华为', '华为旗舰店', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1002, 2002, '小米12 Pro', 3999.00, 50, '手机', '小米', '小米旗舰店', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1003, 2003, 'iPhone 13', 5999.00, 200, '手机', 'Apple', 'Apple官网', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试用户（密码：123456 BCrypt加密）
INSERT INTO tb_user (id, username, password, phone, nick_name, status, created, updated)
VALUES
(1, 'testuser', '$2a$10$YOUR_ENCODED_PASSWORD_HERE', '13800138000', '测试用户', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
