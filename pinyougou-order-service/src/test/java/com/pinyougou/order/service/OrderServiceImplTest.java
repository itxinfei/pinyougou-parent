package com.pinyougou.order.service;

import com.pinyougou.exception.ResourceNotFoundException;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.order.service.impl.OrderServiceImpl;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbOrderExample;
import com.pinyougou.pojo.TbPayLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import util.IdWorker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 订单服务实现类单元测试
 * <p>
 * 测试覆盖：
 * - 订单查询：findAll、findOne、findPage
 * - 订单创建：add（参数校验）
 * - 订单更新：update
 * - 订单删除：delete
 * - 支付状态更新：updateOrderStatus
 * - 支付日志查询：searchPayLogFromRedis
 * - 金额计算精度验证
 * <p>
 * 测试策略：
 * - 使用Mockito模拟所有Mapper和Redis依赖
 * - 使用@Mock标注Mock对象，@InjectMocks自动注入
 * - 使用MockitoJUnitRunner.Silent.class避免strict stubbing检查
 * - 每个测试方法独立，不依赖其他测试的执行顺序
 * <p>
 * Mock对象说明：
 * - orderMapper: 订单数据访问层
 * - payLogMapper: 支付日志数据访问层
 * - itemMapper: 商品数据访问层
 * - orderItemMapper: 订单项数据访问层
 * - idWorker: 分布式ID生成器
 * - redisTemplate: Redis缓存操作
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class OrderServiceImplTest {

    @Mock
    private TbOrderMapper orderMapper;

    @Mock
    private TbPayLogMapper payLogMapper;

    @Mock
    private TbItemMapper itemMapper;

    @Mock
    private TbOrderItemMapper orderItemMapper;

    @Mock
    private IdWorker idWorker;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate redisTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private TbOrder testOrder;
    private TbPayLog testPayLog;

    /**
     * 测试前置准备
     * <p>
     * 初始化测试数据：
     * - testOrder: 测试订单（ID=123456789, 用户=test_user_001, 金额=299.99元）
     * - testPayLog: 测试支付日志（流水号=PAY123456, 金额=29999分）
     */
    @Before
    public void setUp() {
        // 准备测试订单数据
        testOrder = new TbOrder();
        testOrder.setOrderId(123456789L);
        testOrder.setUserId("test_user_001");
        testOrder.setPaymentType("1");
        testOrder.setStatus("1");
        testOrder.setReceiver("张三");
        testOrder.setReceiverMobile("13800138000");
        testOrder.setReceiverAreaName("北京市朝阳区");
        testOrder.setPayment(new BigDecimal("299.99"));

        // 准备测试支付日志数据
        testPayLog = new TbPayLog();
        testPayLog.setOutTradeNo("PAY123456");
        testPayLog.setUserId("test_user_001");
        testPayLog.setTotalFee(29999L);
        testPayLog.setTradeState("0");
    }

    /**
     * 测试查询全部订单
     * <p>
     * 验证：
     * 1. 返回的订单列表不为null
     * 2. 订单数量与预期一致
     * 3. 订单ID匹配
     * 4. selectByExample方法被正确调用
     */
    @Test
    public void testFindAll() {
        // 准备测试数据
        List<TbOrder> orderList = new ArrayList<>();
        orderList.add(testOrder);

        // Mock行为：selectByExample返回订单列表
        Mockito.when(orderMapper.selectByExample(null)).thenReturn(orderList);

        // 执行测试
        List<TbOrder> result = orderService.findAll();

        // 验证结果
        assertNotNull("订单列表不应为null", result);
        assertEquals("订单数量应为1", 1, result.size());
        assertEquals("订单ID不匹配", testOrder.getOrderId(), result.get(0).getOrderId());

        // 验证方法调用
        Mockito.verify(orderMapper).selectByExample(null);
    }

    /**
     * 测试按ID查询订单
     * <p>
     * 验证：
     * 1. 返回的订单不为null
     * 2. 订单ID与查询ID一致
     * 3. selectByPrimaryKey方法被正确调用
     */
    @Test
    public void testFindOne() {
        Long orderId = 123456789L;

        // Mock行为：selectByPrimaryKey返回测试订单
        Mockito.when(orderMapper.selectByPrimaryKey(orderId)).thenReturn(testOrder);

        // 执行测试
        TbOrder result = orderService.findOne(orderId);

        // 验证结果
        assertNotNull("订单不应为null", result);
        assertEquals("订单ID不匹配", orderId, result.getOrderId());

        // 验证方法调用
        Mockito.verify(orderMapper).selectByPrimaryKey(orderId);
    }

    /**
     * 测试查询不存在的订单
     * <p>
     * 验证：当订单不存在时，findOne返回null（不抛异常）
     * <p>
     * 注意：当前实现中findOne直接返回selectByPrimaryKey的结果，
     * 不会对null进行特殊处理
     */
    @Test
    public void testFindOneNotFound() {
        Long orderId = 999999L;

        // Mock行为：selectByPrimaryKey返回null
        Mockito.when(orderMapper.selectByPrimaryKey(orderId)).thenReturn(null);

        // 执行测试（findOne返回null，不抛异常）
        TbOrder result = orderService.findOne(orderId);
        assertNull("不存在的订单应返回null", result);
    }

    /**
     * 测试支付状态更新
     * <p>
     * 验证：
     * 1. 支付日志状态更新为"1"（已支付）
     * 2. 交易流水号正确设置
     * 3. 支付日志和订单的update方法被调用
     * 4. Redis中的支付日志被清除
     * <p>
     * 测试场景：微信支付回调后，更新订单状态
     * 执行流程：
     * 1. 查询支付日志
     * 2. 更新支付日志状态
     * 3. 解析订单列表并逐个更新
     * 4. 清除Redis缓存
     */
    @Test
    public void testUpdateOrderStatus() {
        String outTradeNo = "PAY123456";
        String transactionId = "WXTRANSACTION123";

        // 设置订单列表（updateOrderStatus需要解析此字段）
        testPayLog.setOrderList(testOrder.getOrderId() + "");

        // Mock支付日志查询
        Mockito.when(payLogMapper.selectByPrimaryKey(outTradeNo)).thenReturn(testPayLog);

        // Mock订单查询（updateOrderStatus会逐个查询订单）
        Mockito.when(orderMapper.selectByPrimaryKey(testOrder.getOrderId())).thenReturn(testOrder);

        // Mock Redis操作 - 需要设置BoundHashOperations返回值
        org.springframework.data.redis.core.BoundHashOperations boundHashOps =
            Mockito.mock(org.springframework.data.redis.core.BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("payLog")).thenReturn(boundHashOps);

        // 执行测试
        orderService.updateOrderStatus(outTradeNo, transactionId);

        // 验证支付日志查询和更新
        Mockito.verify(payLogMapper).selectByPrimaryKey(outTradeNo);
        Mockito.verify(payLogMapper).updateByPrimaryKey(testPayLog);

        // 验证订单更新
        Mockito.verify(orderMapper).updateByPrimaryKey(Mockito.any(TbOrder.class));

        // 验证Redis缓存清除
        Mockito.verify(redisTemplate).boundHashOps("payLog");

        // 验证支付日志状态已更新
        assertEquals("支付状态应为已支付", "1", testPayLog.getTradeState());
        assertEquals("交易流水号应匹配", transactionId, testPayLog.getTransactionId());
    }

    /**
     * 测试查询支付日志（Redis中不存在）
     * <p>
     * 验证：当Redis中没有支付日志时，返回null
     * <p>
     * 注意：需要正确设置Redis mock链式调用
     * redisTemplate.boundHashOps("payLog") -> boundHashOps -> .get(userId) -> null
     */
    @Test
    public void testSearchPayLogFromRedisNotFound() {
        String userId = "test_user_001";

        // Mock Redis - 需要设置BoundHashOperations返回值
        org.springframework.data.redis.core.BoundHashOperations boundHashOps =
            Mockito.mock(org.springframework.data.redis.core.BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("payLog")).thenReturn(boundHashOps);
        Mockito.when(boundHashOps.get(userId)).thenReturn(null);

        // 执行测试
        TbPayLog result = orderService.searchPayLogFromRedis(userId);

        // 验证结果
        assertNull("支付日志应为null", result);
    }

    /**
     * 测试查询支付日志（Redis中存在）
     * <p>
     * 验证：当Redis中有支付日志时，返回正确的支付日志对象
     * <p>
     * 注意：需要正确设置Redis mock链式调用
     * redisTemplate.boundHashOps("payLog") -> boundHashOps -> .get(userId) -> testPayLog
     */
    @Test
    public void testSearchPayLogFromRedisFound() {
        String userId = "test_user_001";

        // Mock Redis - 需要设置BoundHashOperations返回值
        org.springframework.data.redis.core.BoundHashOperations boundHashOps =
            Mockito.mock(org.springframework.data.redis.core.BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("payLog")).thenReturn(boundHashOps);
        Mockito.when(boundHashOps.get(userId)).thenReturn(testPayLog);

        // 执行测试
        TbPayLog result = orderService.searchPayLogFromRedis(userId);

        // 验证结果
        assertNotNull("支付日志不应为null", result);
        assertEquals("支付日志ID不匹配", testPayLog.getOutTradeNo(), result.getOutTradeNo());
    }

    /**
     * 测试批量删除订单
     * <p>
     * 验证：
     * 1. 删除操作被正确执行
     * 2. deleteByPrimaryKey被调用的次数与ID数组长度一致
     */
    @Test
    public void testDelete() {
        Long[] ids = {123456789L, 987654321L};

        // 执行测试
        orderService.delete(ids);

        // 验证方法调用次数（2个ID，调用2次）
        Mockito.verify(orderMapper, Mockito.times(2)).deleteByPrimaryKey(Mockito.anyLong());
    }

    /**
     * 测试删除空ID数组
     * <p>
     * 验证：当传入空数组时，不执行任何删除操作
     */
    @Test
    public void testDelete_EmptyArray() {
        Long[] ids = {};

        // 执行测试（不应该抛出异常）
        orderService.delete(ids);

        // 验证没有任何删除操作
        Mockito.verify(orderMapper, Mockito.never()).deleteByPrimaryKey(Mockito.anyLong());
    }

    /**
     * 测试金额计算精度
     * <p>
     * 验证：
     * 1. BigDecimal累加计算精确无误
     * 2. 元转分计算正确（乘以100后四舍五入）
     * <p>
     * 测试数据：
     * - 19.99 + 29.99 + 9.99 = 59.97元
     * - 59.97元 = 5997分
     */
    @Test
    public void testAmountCalculationPrecision() {
        // 测试BigDecimal金额计算的精确性
        BigDecimal price1 = new BigDecimal("19.99");
        BigDecimal price2 = new BigDecimal("29.99");
        BigDecimal price3 = new BigDecimal("9.99");

        // 使用BigDecimal累加（避免double精度损失）
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(price1);
        total = total.add(price2);
        total = total.add(price3);

        // 预期结果：59.97
        BigDecimal expected = new BigDecimal("59.97");
        assertEquals("金额计算应精确", 0, total.compareTo(expected));

        // 验证元转分：59.97 * 100 = 5997分
        Long totalFee = total.multiply(new BigDecimal(100))
                          .setScale(0, BigDecimal.ROUND_HALF_UP)
                          .longValue();
        assertEquals("元转分应正确", 5997L, totalFee.longValue());
    }

    /**
     * 测试金额为0的订单（边界条件）
     * <p>
     * 验证：零金额的订单能正确处理，元转分后仍为0
     */
    @Test
    public void testAmountCalculation_Zero() {
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal total = price.add(new BigDecimal("0.00"));

        assertEquals("零金额应正确", 0, total.compareTo(BigDecimal.ZERO));

        // 验证零金额元转分
        Long totalFee = total.multiply(new BigDecimal(100))
                          .setScale(0, BigDecimal.ROUND_HALF_UP)
                          .longValue();
        assertEquals("零金额转分应为0", 0L, totalFee.longValue());
    }

    // ========== 补充的关键业务流程测试 ==========

    /**
     * 测试findPage分页查询
     * <p>
     * 验证：
     * 1. 分页结果不为null
     * 2. 总记录数正确
     * 3. 当前页数据量正确
     * <p>
     * 注意：需要返回Page对象（而非List），因为findPage内部会强制转换为Page
     * PageHelper通过ThreadLocal存储分页信息，mock时需直接返回Page对象
     */
    @Test
    public void testFindPage() {
        int pageNum = 1;
        int pageSize = 10;

        // Mock PageHelper - 直接返回Page对象（非List）
        com.github.pagehelper.Page<TbOrder> page = new com.github.pagehelper.Page<>(pageNum, pageSize);
        List<TbOrder> orderList = new ArrayList<>();
        orderList.add(testOrder);
        page.addAll(orderList);
        page.setTotal(1);

        // 注意：必须匹配null参数（findPage传入null）
        Mockito.when(orderMapper.selectByExample(null)).thenReturn(page);

        // 执行测试
        entity.PageResult result = orderService.findPage(pageNum, pageSize);

        // 验证结果
        assertNotNull("分页结果不应为null", result);
        assertNotNull("商品列表不应为null", result.getRows());
        assertEquals("总记录数应为1", 1L, result.getTotal());
        assertEquals("当前页商品数应为1", 1, result.getRows().size());
    }

    /**
     * 测试add方法参数校验（订单信息为空）
     * <p>
     * 验证：当传入null订单时，抛出ValidationException
     */
    @Test(expected = com.pinyougou.exception.ValidationException.class)
    public void testAdd_NullOrder() {
        orderService.add(null);
    }

    /**
     * 测试add方法参数校验（用户ID为空）
     * <p>
     * 验证：当用户ID为空字符串时，抛出ValidationException
     */
    @Test(expected = com.pinyougou.exception.ValidationException.class)
    public void testAdd_EmptyUserId() {
        TbOrder order = new TbOrder();
        order.setUserId(""); // 空字符串
        order.setPaymentType("1");
        order.setReceiver("张三");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaName("北京市朝阳区");

        orderService.add(order);
    }

    /**
     * 测试update方法（订单状态更新）
     * <p>
     * 验证：
     * 1. updateByPrimaryKey方法被正确调用
     * 2. 传入的订单对象正确
     * <p>
     * 注意：updateByPrimaryKey返回int（受影响行数），Mock时使用thenReturn(1)
     */
    @Test
    public void testUpdate() {
        TbOrder updateOrder = new TbOrder();
        updateOrder.setOrderId(123456789L);
        updateOrder.setStatus("2"); // 更新为已付款

        // Mock updateByPrimaryKey返回int（受影响行数）
        Mockito.when(orderMapper.updateByPrimaryKey(Mockito.any(TbOrder.class))).thenReturn(1);

        // 执行测试
        orderService.update(updateOrder);

        // 验证方法调用
        Mockito.verify(orderMapper).updateByPrimaryKey(updateOrder);
    }

    /**
     * 测试findOne（订单不存在）
     * <p>
     * 验证：当订单ID不存在时，返回null
     * <p>
     * 与testFindOneNotFound测试相同，但使用不同的ID值
     */
    @Test
    public void testFindOne_NotFound() {
        Mockito.when(orderMapper.selectByPrimaryKey(999999L)).thenReturn(null);
        TbOrder result = orderService.findOne(999999L);
        assertNull("不存在的订单应返回null", result);
    }
}
