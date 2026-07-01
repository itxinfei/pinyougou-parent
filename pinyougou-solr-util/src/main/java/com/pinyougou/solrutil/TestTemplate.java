package com.pinyougou.solrutil;

import com.pinyougou.pojo.TbItem;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/applicationContext*.xml")
public class TestTemplate {
    @Autowired
    private SolrTemplate solrTemplate;

    /**
     * 增加（修改）
     */
    @Test
    public void testAdd() {
        TbItem item = new TbItem();
        item.setId(1L);
        item.setBrand("华为");
        item.setCategory("手机");
        item.setGoodsId(1L);
        item.setSeller("华为2号专卖店");
        item.setTitle("华为Mate9");
        item.setPrice(new BigDecimal(2000));
        solrTemplate.saveBean(item);
        solrTemplate.commit();
    }

    /**
     * 按主键查询
     */
    @Test
    public void testFindOne() {
        Logger logger = Logger.getLogger(TestTemplate.class);
        TbItem item = solrTemplate.getById(1, TbItem.class);
        logger.info(item.getTitle());
    }


}
