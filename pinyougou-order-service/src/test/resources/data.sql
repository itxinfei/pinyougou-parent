-- =====================================================
-- 集成测试数据
-- 在每次测试执行前加载，确保数据一致性
-- =====================================================

-- 清空表数据（按依赖顺序）
DELETE FROM tb_order_item;
DELETE FROM tb_pay_log;
DELETE FROM tb_order;
DELETE FROM tb_user;
DELETE FROM tb_item;

-- 重新插入测试数据
INSERT INTO tb_item (id, goods_id, title, price, num, category, brand, seller, status, created, updated)
VALUES
(1001, 2001, '华为P50 Pro', 4999.00, 100, '手机', '华为', '华为旗舰店', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1002, 2002, '小米12 Pro', 3999.00, 50, '手机', '小米', '小米旗舰店', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1003, 2003, 'iPhone 13', 5999.00, 200, '手机', 'Apple', 'Apple官网', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tb_user (id, username, password, phone, nick_name, status, created, updated)
VALUES
(1, 'testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800138000', '测试用户', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
