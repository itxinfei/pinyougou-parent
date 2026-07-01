package com.pinyougou.search.service.impl;

import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.HighlightPageImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 商品搜索服务测试
 * <p>
 * 测试覆盖：
 * - 商品搜索（search）
 * - 商品列表查询（searchList）
 * - 分类列表查询（searchCategoryList）
 * - 品牌和规格查询（searchBrandAndSpecList）
 * - 导入商品到Solr（importList）
 * - 根据SPU删除商品（deleteByGoodsIds）
 *
 * @author Administrator
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemSearchServiceImplTest {

    @Mock
    private SolrTemplate solrTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ItemSearchServiceImpl itemSearchService;

    private TbItem testItem;
    private Map<String, Object> searchMap;

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
        testItem.setPrice(new java.math.BigDecimal("299.99"));
        testItem.setImage("test.jpg");
        testItem.setCategory("手机");
        testItem.setBrand("华为");
        testItem.setSeller("华为旗舰店");

        // 准备搜索条件
        searchMap = new HashMap<>();
        searchMap.put("keywords", "手机");
        searchMap.put("category", "");
        searchMap.put("brand", "");
        searchMap.put("price", "");
        searchMap.put("pageNo", 1);
        searchMap.put("pageSize", 20);
        searchMap.put("sort", "");
        searchMap.put("sortField", "");
    }

    /**
     * 测试商品搜索
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSearch() {
        // Mock Solr高亮结果
        org.springframework.data.solr.core.query.result.HighlightPage<TbItem> highlightPage =
            new org.springframework.data.solr.core.query.result.HighlightPageImpl<>(itemList);

        // Mock高亮字段
        Map<String, Map<String, List<String>>> highlightMap = new HashMap<>();
        Map<String, List<String>> titleHighlight = new HashMap<>();
        titleHighlight.put("title", java.util.Arrays.asList("测试<em>商品</em>"));
        highlightMap.put("title", titleHighlight);
        highlightPage.setHighlights(highlightMap);

        Mockito.when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(highlightPage);

        // Mock分类列表
        List<String> categoryList = new ArrayList<>();
        categoryList.add("手机");
        categoryList.add("电脑");

        org.springframework.data.redis.core.HashOperations hashOps =
            Mockito.mock(org.springframework.data.redis.core.HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.keys("")).thenReturn(categoryList);

        // 执行测试
        Map<String, Object> result = itemSearchService.search(searchMap);

        // 验证结果
        assertNotNull("搜索结果不应为null", result);
        assertNotNull("商品列表不应为null", result.get("rows"));
        assertNotNull("分类列表不应为null", result.get("categoryList"));
        assertEquals("商品数量应为1", 1, ((List<TbItem>) result.get("rows")).size());
        assertEquals("分类数量应为2", 2, ((List<String>) result.get("categoryList")).size());
    }

    /**
     * 测试搜索参数处理
     */
    @Test
    public void testSearchParams() {
        // 测试关键词空格处理 - 应该被trim
        searchMap.put("keywords", "  手机  ");
        Mockito.when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(new org.springframework.data.solr.core.query.result.HighlightPageImpl<>(new ArrayList<>()));
        Map<String, Object> result = itemSearchService.search(searchMap);
        assertEquals("关键词应去除空格", "手机", searchMap.get("keywords"));

        // 测试分页参数默认值 - 当pageNo为null时应默认为1
        searchMap.put("pageNo", null);
        searchMap.put("pageSize", null);
        result = itemSearchService.search(searchMap);
        assertNotNull("搜索结果不应为null", result);
    }

    /**
     * 测试导入商品到Solr
     */
    @Test
    public void testImportList() {
        List<TbItem> itemList = new ArrayList<>();
        itemList.add(testItem);

        // Mock Solr
        Mockito.doNothing().when(solrTemplate).saveBeans(Mockito.anyList());
        Mockito.doNothing().when(solrTemplate).commit();

        // 执行测试
        itemSearchService.importList(itemList);

        // 验证方法调用
        Mockito.verify(solrTemplate).saveBeans(itemList);
        Mockito.verify(solrTemplate).commit();
    }

    /**
     * 测试批量删除商品
     */
    @Test
    public void testDeleteByGoodsIds() {
        List goodsIds = new ArrayList();
        goodsIds.add(2001L);
        goodsIds.add(2002L);

        // Mock Solr
        Mockito.doNothing().when(solrTemplate).delete(Mockito.any());
        Mockito.doNothing().when(solrTemplate).commit();

        // 执行测试
        itemSearchService.deleteByGoodsIds(goodsIds);

        // 验证方法调用
        Mockito.verify(solrTemplate).delete(Mockito.any());
        Mockito.verify(solrTemplate).commit();
    }

    /**
     * 测试空列表导入
     */
    @Test
    public void testImportList_Empty() {
        List<TbItem> emptyList = new ArrayList<>();

        // Mock Solr
        Mockito.doNothing().when(solrTemplate).saveBeans(Mockito.anyList());
        Mockito.doNothing().when(solrTemplate).commit();

        // 执行测试
        itemSearchService.importList(emptyList);

        // 验证方法调用
        Mockito.verify(solrTemplate).saveBeans(emptyList);
        Mockito.verify(solrTemplate).commit();
    }

    /**
     * 测试按分类搜索品牌和规格
     */
    @Test
    public void testSearchBrandAndSpecList() {
        String category = "手机";

        // Mock Redis - 使用HashOperations mock
        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.get(category)).thenReturn(1001L);

        Mockito.when(redisTemplate.boundHashOps("brandList")).thenReturn(hashOps);
        List<Map> brandList = new ArrayList<>();
        Map<String, Object> brand = new HashMap<>();
        brand.put("id", 1L);
        brand.put("name", "华为");
        brandList.add(brand);
        Mockito.when(hashOps.get(1001L)).thenReturn(brandList);

        Mockito.when(redisTemplate.boundHashOps("specList")).thenReturn(hashOps);
        List<Map> specList = new ArrayList<>();
        Map<String, Object> spec = new HashMap<>();
        spec.put("id", 1L);
        spec.put("name", "颜色");
        specList.add(spec);
        Mockito.when(hashOps.get(1001L)).thenReturn(specList);

        // 直接调用包级私有方法（无需反射）
        Map<String, Object> result = itemSearchService.searchBrandAndSpecList(category);

        assertNotNull("结果不应为null", result);
        assertNotNull("品牌列表不应为null", result.get("brandList"));
        assertNotNull("规格列表不应为null", result.get("specList"));
        assertEquals("品牌数量应为1", 1, ((List) result.get("brandList")).size());
        assertEquals("规格数量应为1", 1, ((List) result.get("specList")).size());
    }

    /**
     * 测试分类不存在时的降级处理
     */
    @Test
    public void testSearchBrandAndSpecList_CategoryNotFound() {
        String category = "不存在的分类";

        // Mock Redis返回null
        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.get(category)).thenReturn(null);

        // 直接调用包级私有方法（无需反射）
        Map<String, Object> result = itemSearchService.searchBrandAndSpecList(category);

        assertNotNull("结果不应为null", result);
        assertTrue("品牌列表应为空", result.get("brandList") == null || ((List) result.get("brandList")).isEmpty());
        assertTrue("规格列表应为空", result.get("specList") == null || ((List) result.get("specList")).isEmpty());
    }

    /**
     * 测试品牌列表为空时的降级处理
     */
    @Test
    public void testSearchBrandAndSpecList_BrandListEmpty() {
        String category = "手机";

        // Mock Redis返回模板ID但品牌列表为空
        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.get(category)).thenReturn(1001L);
        Mockito.when(redisTemplate.boundHashOps("brandList")).thenReturn(hashOps);
        Mockito.when(hashOps.get(1001L)).thenReturn(null);

        // 直接调用包级私有方法（无需反射）
        Map<String, Object> result = itemSearchService.searchBrandAndSpecList(category);

        assertNotNull("结果不应为null", result);
        // 应该触发降级日志，但不会抛出异常
    }
}

    // ========== 补充的关键测试场景 ==========

    /**
     * 测试搜索结果为空
     */
    @Test
    public void testSearch_EmptyResult() {
        // Mock Solr返回空结果
        List<TbItem> emptyList = new ArrayList<>();
        Mockito.when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(new HighlightPageImpl<>(emptyList));

        // Mock分类列表
        List<String> categoryList = new ArrayList<>();
        categoryList.add("手机");
        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.keys("")).thenReturn(categoryList);

        // 执行测试
        Map<String, Object> result = itemSearchService.search(searchMap);

        // 验证结果
        assertNotNull("搜索结果不应为null", result);
        assertNotNull("商品列表不应为null", result.get("rows"));
        assertEquals("商品数量应为0", 0, ((List<TbItem>) result.get("rows")).size());
        assertEquals("总页数应为1", 1, result.get("totalPages"));
        assertEquals("总记录数应为0", 0L, result.get("total"));
    }

    /**
     * 测试空关键词搜索
     */
    @Test
    public void testSearch_EmptyKeywords() {
        searchMap.put("keywords", "");

        // Mock Solr返回空结果
        List<TbItem> emptyList = new ArrayList<>();
        Mockito.when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(new HighlightPageImpl<>(emptyList));

        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.keys("")).thenReturn(new ArrayList<>());

        // 执行测试
        Map<String, Object> result = itemSearchService.search(searchMap);

        // 验证结果（应该能正确处理空关键词）
        assertNotNull("搜索结果不应为null", result);
    }

    /**
     * 测试导入商品到Solr（空列表）
     */
    @Test
    public void testImportList_Empty() {
        List<TbItem> emptyList = new ArrayList<>();

        // Mock Solr
        Mockito.doNothing().when(solrTemplate).saveBeans(Mockito.anyList());
        Mockito.doNothing().when(solrTemplate).commit();

        // 执行测试
        itemSearchService.importList(emptyList);

        // 验证方法调用（应该调用commit但不保存任何内容）
        Mockito.verify(solrTemplate).saveBeans(emptyList);
        Mockito.verify(solrTemplate).commit();
    }

    /**
     * 测试删除商品时goodsIds为空
     */
    @Test
    public void testDeleteByGoodsIds_EmptyList() {
        List goodsIds = new ArrayList<>();

        // Mock Solr
        Mockito.doNothing().when(solrTemplate).delete(Mockito.any());
        Mockito.doNothing().when(solrTemplate).commit();

        // 执行测试
        itemSearchService.deleteByGoodsIds(goodsIds);

        // 验证不应该调用删除方法
        Mockito.verify(solrTemplate, Mockito.never()).delete(Mockito.any());
        Mockito.verify(solrTemplate).commit();
    }

    /**
     * 测试删除商品时goodsIds为null
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteByGoodsIds_NullList() {
        // 执行测试（应该抛出异常）
        itemSearchService.deleteByGoodsIds(null);
    }

    /**
     * 测试分页参数为负数
     */
    @Test
    public void testSearch_NegativePage() {
        searchMap.put("pageNo", -1);
        searchMap.put("pageSize", -1);

        // Mock Solr返回空结果
        List<TbItem> emptyList = new ArrayList<>();
        Mockito.when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(new HighlightPageImpl<>(emptyList));

        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.keys("")).thenReturn(new ArrayList<>());

        // 执行测试（应该能正确处理负数并转换为正数）
        Map<String, Object> result = itemSearchService.search(searchMap);
        assertNotNull("搜索结果不应为null", result);
    }

    /**
     * 测试关键字去除前后空格
     */
    @Test
    public void testSearch_TrimKeywords() {
        searchMap.put("keywords", "  手机  ");

        // Mock Solr返回空结果
        List<TbItem> emptyList = new ArrayList<>();
        Mockito.when(solrTemplate.queryForHighlightPage(Mockito.any(), Mockito.eq(TbItem.class)))
            .thenReturn(new HighlightPageImpl<>(emptyList));

        HashOperations hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("itemCat")).thenReturn(hashOps);
        Mockito.when(hashOps.keys("")).thenReturn(new ArrayList<>());

        // 执行测试
        Map<String, Object> result = itemSearchService.search(searchMap);

        // 验证关键词已trim
        assertEquals("关键词应去除空格", "手机", searchMap.get("keywords"));
    }
}
