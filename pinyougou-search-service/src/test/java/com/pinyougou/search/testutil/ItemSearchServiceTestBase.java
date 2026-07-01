package com.pinyougou.search.testutil;

import com.pinyougou.pojo.TbItem;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.HighlightPageImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ItemSearchServiceImpl测试工具类
 * <p>
 * 提供商品搜索服务测试的公共方法：
 * 1. 创建测试用的商品数据
 * 2. 创建测试用的搜索条件
 * 3. Mock Solr和Redis的常用操作
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class ItemSearchServiceTestBase {

    /**
     * 创建测试商品（默认数据）
     *
     * @return TbItem对象
     */
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

    /**
     * 创建测试商品（自定义数据）
     *
     * @param id 商品ID
     * @param title 商品标题
     * @param price 商品价格
     * @param category 分类
     * @param brand 品牌
     * @return TbItem对象
     */
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

    /**
     * 创建测试商品列表
     *
     * @param count 数量
     * @return 商品列表
     */
    protected List<TbItem> createTestItemList(int count) {
        List<TbItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(createTestItem());
        }
        return items;
    }

    /**
     * 创建默认搜索条件
     *
     * @return 搜索条件Map
     */
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

    /**
     * Mock Solr高亮搜索结果
     *
     * @param solrTemplate SolrTemplate
     * @param items 商品列表
     * @return HighlightPage对象
     */
    @SuppressWarnings("unchecked")
    protected HighlightPage<TbItem> mockSolrHighlightResult(SolrTemplate solrTemplate, List<TbItem> items) {
        HighlightPage<TbItem> highlightPage = new HighlightPageImpl<>(items);
        return highlightPage;
    }

    /**
     * Mock Solr高亮搜索结果（带高亮字段）
     *
     * @param solrTemplate SolrTemplate
     * @param items 商品列表
     * @param highlightField 高亮字段名
     * @param highlightValues 高亮值列表
     * @return HighlightPage对象
     */
    @SuppressWarnings("unchecked")
    protected HighlightPage<TbItem> mockSolrHighlightResultWithHighlights(
            SolrTemplate solrTemplate, List<TbItem> items,
            String highlightField, List<String> highlightValues) {
        HighlightPage<TbItem> highlightPage = new HighlightPageImpl<>(items);

        Map<String, Map<String, List<String>>> highlightMap = new HashMap<>();
        Map<String, List<String>> fieldHighlight = new HashMap<>();
        fieldHighlight.put(highlightField, highlightValues);
        highlightMap.put(highlightField, fieldHighlight);
        highlightPage.setHighlights(highlightMap);

        when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(highlightPage);
        return highlightPage;
    }

    /**
     * Mock Redis分类列表
     *
     * @param redisTemplate RedisTemplate
     * @param categories 分类列表
     */
    protected void mockCategoryList(RedisTemplate<String, Object> redisTemplate, List<String> categories) {
        org.springframework.data.redis.core.HashOperations hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        when(hashOps.keys("")).thenReturn(categories);
    }

    /**
     * Mock Redis品牌和规格列表
     *
     * @param redisTemplate RedisTemplate
     * @param templateId 模板ID
     * @param brandList 品牌列表
     * @param specList 规格列表
     */
    protected void mockBrandAndSpecList(RedisTemplate<String, Object> redisTemplate,
                                        Long templateId, List<Map> brandList, List<Map> specList) {
        org.springframework.data.redis.core.HashOperations hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        when(hashOps.get(Mockito.anyString())).thenReturn(templateId);
        when(redisTemplate.boundHashOps("brandList")).thenReturn(hashOps);
        when(hashOps.get(templateId)).thenReturn(brandList);
        when(redisTemplate.boundHashOps("specList")).thenReturn(hashOps);
        when(hashOps.get(templateId)).thenReturn(specList);
    }

    /**
     * 验证搜索结果包含必要字段
     *
     * @param result 搜索结果
     */
    protected void verifySearchResult(Map<String, Object> result) {
        assertNotNull("搜索结果不应为null", result);
        assertNotNull("商品列表不应为null", result.get("rows"));
        assertNotNull("总页数不应为null", result.get("totalPages"));
        assertNotNull("总记录数不应为null", result.get("total"));
    }
}
