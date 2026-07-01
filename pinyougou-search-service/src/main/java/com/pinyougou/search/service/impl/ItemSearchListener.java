package com.pinyougou.search.service.impl;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;

@Component
public class ItemSearchListener implements MessageListener {

    private static final Logger logger = Logger.getLogger(ItemSearchListener.class);

    @Autowired
    private ItemSearchService itemSearchService;

    /**
     *
     */
    @Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
        try {
            String text = textMessage.getText();//json字符串
            logger.info("监听到消息:" + text);

            List<TbItem> itemList = JSON.parseArray(text, TbItem.class);
            itemSearchService.importList(itemList);
            logger.info("导入到solr索引库");
        } catch (JMSException e) {
            logger.error("商品搜索索引导入监听处理失败", e);
        }
    }
}
