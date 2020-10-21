package com.pinyougou.solrutil;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojo.TbItemExample.Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class SolrUtil {

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private SolrTemplate solrTemplate;

    /**
     * 导入商品数据
     * item_updatetime
     */
    public void importItemData() throws UnsupportedEncodingException {
        TbItemExample example = new TbItemExample();
        Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1");//审核通过的才导入的
        List<TbItem> itemList = itemMapper.selectByExample(example);
        System.out.println("---商品列表---");
        for (TbItem item : itemList) {
            //打印商品列表
            System.out.println(item.getId() + " " + item.getTitle() + " " + item.getPrice() + "" + item.getUpdateTime());
            //设置编码UTF-8
            String spec = item.getSpec();
            spec.getBytes("UTF-8");
            Map specMap = JSON.parseObject(spec, Map.class);//从数据库中提取规格json字符串转换为map
            item.setSpecMap(specMap);
        }
        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();
        System.out.println("---结束---");
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
        SolrUtil solrUtil = (SolrUtil) context.getBean("solrUtil");
        solrUtil.importItemData();
    }
}
