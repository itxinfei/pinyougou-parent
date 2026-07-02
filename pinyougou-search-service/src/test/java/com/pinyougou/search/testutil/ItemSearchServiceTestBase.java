package com.pinyougou.search.testutil;

import com.pinyougou.pojo.TbItem;
import org.mockito.Mockito;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ItemSearchServiceTestBase {

    protected TbItem createTestItem() {
        TbItem item = new TbItem();
        item.setId(1001L);
        item.setGoodsId(2001L);
        item.setTitle("测试商品");
        item.setPrice(new BigDecimal("299.99"));
        item.setImage("test.jpg");
        item.setCategory("手机");
        item.setBrand("华为");
        item.setSeller("华为旗舰店");
        item.setStatus("1");
        return item;
    }

    protected TbItem createTestItem(Long id, String title, BigDecimal price,
                                     String category, String brand) {
        TbItem item = new TbItem();
        item.setId(id);
        item.setGoodsId(id);
        item.setTitle(title);
        item.setPrice(price);
        item.setImage("test.jpg");
        item.setCategory(category);
        item.setBrand(brand);
        item.setSeller(brand + "旗舰店");
        item.setStatus("1");
        return item;
    }

    protected List<TbItem> createTestItemList(int count) {
        List<TbItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(createTestItem());
        }
        return items;
    }

    protected Map<String, Object> createDefaultSearchMap() {
        Map<String, Object> searchMap = new HashMap<>();
        searchMap.put("keywords", "手机");
        searchMap.put("category", "");
        searchMap.put("brand", "");
        searchMap.put("price", "");
        searchMap.put("pageNo", 1);
        searchMap.put("pageSize", 20);
        searchMap.put("sort", "");
        searchMap.put("sortField", "");
        return searchMap;
    }

    @SuppressWarnings("unchecked")
    protected BoundHashOperations<String, Object, Object> mockBoundHash(
            RedisTemplate<String, Object> redisTemplate, String key) {
        BoundHashOperations<String, Object, Object> hashOps = mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps(key)).thenReturn(hashOps);
        return hashOps;
    }

    @SuppressWarnings("unchecked")
    protected void mockCategoryList(RedisTemplate<String, Object> redisTemplate, List<String> categories) {
        BoundHashOperations<String, Object, Object> hashOps = mockBoundHash(redisTemplate, "itemCat");
        when(hashOps.keys()).thenReturn(new HashSet<>(categories));
    }

    @SuppressWarnings("unchecked")
    protected void mockBrandAndSpecList(RedisTemplate<String, Object> redisTemplate,
                                        Long templateId, List<Map> brandList, List<Map> specList) {
        BoundHashOperations<String, Object, Object> hashOps = mockBoundHash(redisTemplate, "itemCat");
        when(hashOps.get(Mockito.anyString())).thenReturn(templateId);
        when(mockBoundHash(redisTemplate, "brandList").get(templateId)).thenReturn(brandList);
        when(mockBoundHash(redisTemplate, "specList").get(templateId)).thenReturn(specList);
    }

    protected void verifySearchResult(Map<String, Object> result) {
        assertNotNull("搜索结果不应为null", result);
        assertNotNull("商品列表不应为null", result.get("rows"));
    }
}
