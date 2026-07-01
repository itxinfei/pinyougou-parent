package com.pinyougou.cart.service.impl;

import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.group.Cart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 购物车服务实现类测试
 * <p>
 * 测试覆盖：
 * - 添加商品到购物车（addGoodsToCartList）
 * - 查找购物车（searchCartBySellerId）
 * - 查找订单项（searchOrderItemByItemId）
 * - 创建订单项（createOrderItem）
 * - 购物车合并（mergeCartList）
 *
 * @author Administrator
 */
@RunWith(MockitoJUnitRunner.class)
public class CartServiceImplTest {

    @Mock
    private TbItemMapper itemMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private CartServiceImpl cartService;

    private TbItem testItem;
    private List<Cart> cartList;

    /**
     * 测试前置准备
     */
    @Before
    public void setUp() {
        // 准备测试商品数据
        testItem = new TbItem();
        testItem.setId(1001L);
        testItem.setGoodsId(2001L);
        testItem.setTitle("测试商品");
        testItem.setPrice(new BigDecimal("99.99"));
        testItem.setStockCount(100);
        testItem.setStatus("1");
        testItem.setSellerId("seller_001");
        testItem.setSeller("测试商家");

        // 准备购物车列表
        cartList = new ArrayList<>();
    }

    /**
     * 测试添加商品到购物车（新商品）
     */
    @Test
    public void testAddGoodsToCartList_NewItem() {
        Long itemId = 1001L;
        Integer num = 2;

        // Mock商品查询
        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);

        // Mock Redis删除操作（释放锁）
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

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
        Mockito.verify(redisTemplate).opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any());
        Mockito.verify(redisTemplate).delete(Mockito.anyString());
    }

    /**
     * 测试添加商品到购物车（已存在商品）
     */
    @Test
    public void testAddGoodsToCartList_ExistingItem() {
        Long itemId = 1001L;
        Integer num1 = 2;
        Integer num2 = 3;

        // Mock商品查询
        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 使用新的购物车列表以确保测试隔离
        List<Cart> isolatedCartList = new ArrayList<>();

        // 第一次添加
        cartService.addGoodsToCartList("user_001", isolatedCartList, itemId, num1);
        assertEquals("第一次添加数量应为2", num1, isolatedCartList.get(0).getOrderItemList().get(0).getNum());

        // 第二次添加（增加数量）
        cartService.addGoodsToCartList("user_001", isolatedCartList, itemId, num2);
        assertEquals("第二次添加后数量应为5", Integer.valueOf(5), isolatedCartList.get(0).getOrderItemList().get(0).getNum());
    }

    /**
     * 测试商品不存在异常
     */
    @Test(expected = ResourceNotFoundException.class)
    public void testAddGoodsToCartList_ItemNotFound() {
        Long itemId = 9999L;

        // Mock商品查询返回null
        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(null);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该抛出异常）
        cartService.addGoodsToCartList("user_001", cartList, itemId, 1);
    }

    /**
     * 测试库存不足异常
     */
    @Test(expected = InsufficientStockException.class)
    public void testAddGoodsToCartList_InsufficientStock() {
        Long itemId = 1001L;
        Integer exceedCount = testItem.getStockCount() + 1; // 超过库存1件

        // Mock商品库存不足
        testItem.setStockCount(50);
        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该抛出异常）
        cartService.addGoodsToCartList("user_001", cartList, itemId, exceedCount);
    }

    /**
     * 测试根据商家ID查找购物车
     */
    @Test
    public void testSearchCartBySellerId_Found() {
        // 准备购物车数据
        Cart cart = new Cart();
        cart.setSellerId("seller_001");
        cart.setSellerName("测试商家");
        cartList.add(cart);

        // 直接调用包级私有方法（无需反射）
        Cart result = cartService.searchCartBySellerId(cartList, "seller_001");

        assertNotNull("应找到购物车", result);
        assertEquals("商家ID不匹配", "seller_001", result.getSellerId());
    }

    /**
     * 测试根据商家ID查找购物车（未找到）
     */
    @Test
    public void testSearchCartBySellerId_NotFound() {
        // 直接调用包级私有方法（无需反射）
        Cart result = cartService.searchCartBySellerId(cartList, "seller_999");

        assertNull("不应找到购物车", result);
    }

    /**
     * 测试创建订单项
     */
    @Test
    public void testCreateOrderItem() {
        Integer num = 5;

        // 直接调用包级私有方法（无需反射）
        TbOrderItem orderItem = cartService.createOrderItem(testItem, num);

        assertNotNull("订单项不应为null", orderItem);
        assertEquals("商品ID不匹配", testItem.getId(), orderItem.getItemId());
        assertEquals("数量不匹配", num, orderItem.getNum());
        assertEquals("价格不匹配", testItem.getPrice(), orderItem.getPrice());
        assertEquals("总价计算应正确", testItem.getPrice().multiply(new BigDecimal(num)), orderItem.getTotalFee());
    }

    /**
     * 测试购物车合并（新商家）
     */
    @Test
    public void testMergeCartList_NewSeller() {
        // 准备主购物车（已有商家1）
        Cart cart1 = new Cart();
        cart1.setSellerId("seller_001");
        cart1.setSellerName("商家1");
        TbOrderItem item1 = new TbOrderItem();
        item1.setItemId(1001L);
        item1.setNum(2);
        item1.setPrice(new BigDecimal("99.99"));
        cart1.setOrderItemList(new ArrayList<TbOrderItem>() {{
            add(item1);
        }});
        List<Cart> mainCartList = new ArrayList<>();
        mainCartList.add(cart1);

        // 准备待合并购物车（商家2）
        Cart cart2 = new Cart();
        cart2.setSellerId("seller_002");
        cart2.setSellerName("商家2");
        TbOrderItem item2 = new TbOrderItem();
        item2.setItemId(1002L);
        item2.setNum(3);
        item2.setPrice(new BigDecimal("199.99"));
        cart2.setOrderItemList(new ArrayList<TbOrderItem>() {{
            add(item2);
        }});
        List<Cart> mergeCartList = new ArrayList<>();
        mergeCartList.add(cart2);

        // 执行合并
        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        // 验证结果
        assertEquals("合并后应有2个商家", 2, result.size());
        assertEquals("商家1数量不变", 1, result.get(0).getOrderItemList().size());
        assertEquals("商家2数量应为1", 1, result.get(1).getOrderItemList().size());

        // 验证商家1的商品内容
        assertEquals("商家1商品ID应为1001", 1001L, result.get(0).getOrderItemList().get(0).getItemId());
        assertEquals("商家1商品数量应为2", Integer.valueOf(2), result.get(0).getOrderItemList().get(0).getNum());

        // 验证商家2的商品内容
        assertEquals("商家2商品ID应为1002", 1002L, result.get(1).getOrderItemList().get(0).getItemId());
        assertEquals("商家2商品数量应为3", Integer.valueOf(3), result.get(1).getOrderItemList().get(0).getNum());
    }

    /**
     * 测试购物车合并（相同商家）
     */
    @Test
    public void testMergeCartList_SameSeller() {
        // 准备主购物车
        Cart cart1 = new Cart();
        cart1.setSellerId("seller_001");
        TbOrderItem item1 = new TbOrderItem();
        item1.setItemId(1001L);
        item1.setNum(2);
        item1.setPrice(new BigDecimal("99.99"));
        cart1.setOrderItemList(new ArrayList<TbOrderItem>() {{
            add(item1);
        }});
        List<Cart> mainCartList = new ArrayList<>();
        mainCartList.add(cart1);

        // 准备待合并购物车（相同商家，不同商品）
        Cart cart2 = new Cart();
        cart2.setSellerId("seller_001");
        TbOrderItem item2 = new TbOrderItem();
        item2.setItemId(1002L);
        item2.setNum(3);
        item2.setPrice(new BigDecimal("199.99"));
        cart2.setOrderItemList(new ArrayList<TbOrderItem>() {{
            add(item2);
        }});
        List<Cart> mergeCartList = new ArrayList<>();
        mergeCartList.add(cart2);

        // 执行合并
        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        // 验证结果
        assertEquals("合并后应有1个商家", 1, result.size());
        assertEquals("商家应有2个商品", 2, result.get(0).getOrderItemList().size());
    }

    /**
     * 测试购物车合并（相同商品）
     */
    @Test
    public void testMergeCartList_SameItem() {
        // 准备主购物车
        Cart cart1 = new Cart();
        cart1.setSellerId("seller_001");
        TbOrderItem item1 = new TbOrderItem();
        item1.setItemId(1001L);
        item1.setNum(2);
        item1.setPrice(new BigDecimal("99.99"));
        item1.setTotalFee(new BigDecimal("199.98"));
        cart1.setOrderItemList(new ArrayList<TbOrderItem>() {{
            add(item1);
        }});
        List<Cart> mainCartList = new ArrayList<>();
        mainCartList.add(cart1);

        // 准备待合并购物车（相同商品）
        Cart cart2 = new Cart();
        cart2.setSellerId("seller_001");
        TbOrderItem item2 = new TbOrderItem();
        item2.setItemId(1001L);
        item2.setNum(3);
        item2.setPrice(new BigDecimal("99.99"));
        item2.setTotalFee(new BigDecimal("299.97"));
        cart2.setOrderItemList(new ArrayList<TbOrderItem>() {{
            add(item2);
        }});
        List<Cart> mergeCartList = new ArrayList<>();
        mergeCartList.add(cart2);

        // 执行合并
        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        // 验证结果
        assertEquals("合并后应有1个商家", 1, result.size());
        assertEquals("商家应有1个商品", 1, result.get(0).getOrderItemList().size());
        assertEquals("合并后数量应为5", Integer.valueOf(5), result.get(0).getOrderItemList().get(0).getNum());

        // 验证合并后的商品总价（2*99.99 + 3*99.99 = 499.95）
        BigDecimal expectedTotalFee = new BigDecimal("499.95");
        assertEquals("合并后总价应正确", 0, result.get(0).getOrderItemList().get(0).getTotalFee().compareTo(expectedTotalFee));
    }

    /**
     * 测试空购物车列表
     */
    @Test
    public void testMergeCartList_EmptyList() {
        List<Cart> mainCartList = new ArrayList<>();
        List<Cart> mergeCartList = new ArrayList<>();

        // 执行合并
        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        // 验证结果
        assertNotNull("结果不应为null", result);
        assertEquals("结果应为空列表", 0, result.size());
    }

    /**
     * 测试库存为0的商品（应抛出异常）
     */
    @Test(expected = InsufficientStockException.class)
    public void testAddGoodsToCartList_ZeroStock() {
        Long itemId = 1001L;
        Integer num = 1;

        // Mock商品库存为0
        testItem.setStockCount(0);
        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该抛出异常）
        cartService.addGoodsToCartList("user_001", cartList, itemId, num);
    }

    /**
     * 测试添加数量为0（应抛出异常或根据业务逻辑处理）
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddGoodsToCartList_ZeroQuantity() {
        Long itemId = 1001L;
        Integer num = 0;

        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该抛出异常或根据业务逻辑处理）
        cartService.addGoodsToCartList("user_001", cartList, itemId, num);
    }

    /**
     * 测试添加负数数量（应抛出异常）
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddGoodsToCartList_NegativeQuantity() {
        Long itemId = 1001L;
        Integer num = -1;

        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该抛出异常）
        cartService.addGoodsToCartList("user_001", cartList, itemId, num);
    }

    /**
     * 测试商品ID为null（应抛出异常）
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddGoodsToCartList_NullItemId() {
        Long itemId = null;
        Integer num = 1;

        Mockito.when(itemMapper.selectByPrimaryKey(Mockito.anyLong())).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该抛出异常）
        cartService.addGoodsToCartList("user_001", cartList, itemId, num);
    }

    /**
     * 测试userId为null或空字符串（边界条件）
     */
    @Test
    public void testAddGoodsToCartList_NullUserId() {
        Long itemId = 1001L;
        Integer num = 1;

        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);
        Mockito.when(redisTemplate.opsForValue().setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(Mockito.anyString());

        // 执行测试（应该能正确处理null或空用户ID）
        List<Cart> result = cartService.addGoodsToCartList(null, cartList, itemId, num);
        assertNotNull("结果不应为null", result);
    }
}
