package com.pinyougou.cart.service.impl;

import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.group.Cart;
import com.pinyougou.cart.testutil.CartServiceTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 购物车服务实现类单元测试
 * <p>
 * 测试覆盖：
 * - 购物车合并：mergeCartList（空主购物车、空合并购物车）
 * - 商品添加：addGoodsToCartList（新商品添加）
 * <p>
 * 测试策略：
 * - 使用Mockito模拟Redis和Mapper依赖
 * - 使用@Mock标注Mock对象，@InjectMocks自动注入
 * - 继承CartServiceTestBase获取测试工具方法
 * <p>
 * Mock对象说明：
 * - itemMapper: 商品数据访问层
 * - redisTemplate: Redis缓存（购物车存储）
 * - valueOperations: Redis Value操作（分布式锁）
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
@RunWith(MockitoJUnitRunner.class)
public class CartServiceImplTest extends CartServiceTestBase {

    @Mock
    private TbItemMapper itemMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CartServiceImpl cartService;

    private TbItem testItem;
    private List<Cart> cartList;

    /**
     * 测试前置准备
     * <p>
     * 初始化Mock对象和测试数据：
     * - 初始化Mockito注解
     * - 创建测试商品
     * - Mock Redis分布式锁获取成功
     */
    @Before
    public void setUp() {
        initMocks();
        testItem = createTestItem();
        cartList = new ArrayList<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any())).thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(anyString());
    }

    /**
     * 测试购物车合并（主购物车为空）
     * <p>
     * 验证：
     * 1. 合并后购物车包含合并购物车的商品
     * 2. 商品数量正确
     * <p>
     * 场景：用户未登录时购物车为空，登录后合并本地购物车
     */
    @Test
    public void testMergeCartList_EmptyMainCart() {
        // 准备合并购物车数据
        Cart cart2 = new Cart();
        cart2.setSellerId("seller_001");
        TbOrderItem item2 = new TbOrderItem();
        item2.setItemId(1001L);
        item2.setNum(3);
        item2.setPrice(new BigDecimal("99.99"));
        List<TbOrderItem> items2 = new ArrayList<>();
        items2.add(item2);
        cart2.setOrderItemList(items2);
        List<Cart> mergeCartList = new ArrayList<>();
        mergeCartList.add(cart2);

        // 主购物车为空
        List<Cart> mainCartList = new ArrayList<>();

        // 执行测试
        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        // 验证结果
        assertEquals("合并后应有1个商家", 1, result.size());
        assertEquals("商品数量应为1", 1, result.get(0).getOrderItemList().size());
        assertEquals("商品数量应为3", Integer.valueOf(3), result.get(0).getOrderItemList().get(0).getNum());
    }

    /**
     * 测试购物车合并（合并购物车为空）
     * <p>
     * 验证：
     * 1. 合并后购物车保持主购物车的商品
     * 2. 商品数量正确
     * <p>
     * 场景：合并购物车为空，直接返回主购物车
     */
    @Test
    public void testMergeCartList_EmptyMergeCart() {
        // 准备主购物车数据
        Cart cart1 = new Cart();
        cart1.setSellerId("seller_001");
        TbOrderItem item1 = new TbOrderItem();
        item1.setItemId(1001L);
        item1.setNum(2);
        item1.setPrice(new BigDecimal("99.99"));
        List<TbOrderItem> items1 = new ArrayList<>();
        items1.add(item1);
        cart1.setOrderItemList(items1);
        List<Cart> mainCartList = new ArrayList<>();
        mainCartList.add(cart1);

        // 合并购物车为空
        List<Cart> mergeCartList = new ArrayList<>();

        // 执行测试
        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        // 验证结果
        assertEquals("合并后应有1个商家", 1, result.size());
        assertEquals("商品数量应为1", 1, result.get(0).getOrderItemList().size());
        assertEquals("商品数量应为2", Integer.valueOf(2), result.get(0).getOrderItemList().get(0).getNum());
    }

    /**
     * 测试添加商品到购物车（新商品）
     * <p>
     * 验证：
     * 1. 商品能成功添加到购物车
     * 2. 购物车中包含正确的商家
     * 3. 商品数量正确
     * 4. selectByPrimaryKey方法被调用
     * <p>
     * 场景：用户添加新商品到空购物车
     */
    @Test
    public void testAddGoodsToCartList_NewItem() {
        Long itemId = 1001L;
        Integer num = 2;

        // Mock商品查询
        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);

        // 执行测试
        List<Cart> result = cartService.addGoodsToCartList("user_001", cartList, itemId, num);

        // 验证结果
        assertNotNull("购物车列表不应为null", result);
        assertEquals("购物车应有1个商家", 1, result.size());
        assertEquals("商家ID不匹配", testItem.getSellerId(), result.get(0).getSellerId());
        assertEquals("商品数量应为1", 1, result.get(0).getOrderItemList().size());
        assertEquals("购买数量不匹配", num, result.get(0).getOrderItemList().get(0).getNum());

        // 验证方法调用
        Mockito.verify(itemMapper).selectByPrimaryKey(itemId);
    }
}
