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

    @Before
    public void setUp() {
        initMocks();
        testItem = createTestItem();
        cartList = new ArrayList<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any())).thenReturn(true);
        Mockito.doNothing().when(redisTemplate).delete(anyString());
    }

    @Test
    public void testMergeCartList_EmptyMainCart() {
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

        List<Cart> mainCartList = new ArrayList<>();

        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        assertEquals("合并后应有1个商家", 1, result.size());
        assertEquals("商品数量应为1", 1, result.get(0).getOrderItemList().size());
        assertEquals("商品数量应为3", Integer.valueOf(3), result.get(0).getOrderItemList().get(0).getNum());
    }

    @Test
    public void testMergeCartList_EmptyMergeCart() {
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

        List<Cart> mergeCartList = new ArrayList<>();

        List<Cart> result = cartService.mergeCartList("user_001", mainCartList, mergeCartList);

        assertEquals("合并后应有1个商家", 1, result.size());
        assertEquals("商品数量应为1", 1, result.get(0).getOrderItemList().size());
        assertEquals("商品数量应为2", Integer.valueOf(2), result.get(0).getOrderItemList().get(0).getNum());
    }

    @Test
    public void testAddGoodsToCartList_NewItem() {
        Long itemId = 1001L;
        Integer num = 2;

        Mockito.when(itemMapper.selectByPrimaryKey(itemId)).thenReturn(testItem);

        List<Cart> result = cartService.addGoodsToCartList("user_001", cartList, itemId, num);

        assertNotNull("购物车列表不应为null", result);
        assertEquals("购物车应有1个商家", 1, result.size());
        assertEquals("商家ID不匹配", testItem.getSellerId(), result.get(0).getSellerId());
        assertEquals("商品数量应为1", 1, result.get(0).getOrderItemList().size());
        assertEquals("购买数量不匹配", num, result.get(0).getOrderItemList().get(0).getNum());

        Mockito.verify(itemMapper).selectByPrimaryKey(itemId);
    }
}
