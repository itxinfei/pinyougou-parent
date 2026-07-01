package com.pinyougou.search.service.impl;

import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pinyougou.search.service.ItemSearchService;

@Component
public class ItemDeleteListener implements MessageListener {

    private static final Logger logger = Logger.getLogger(ItemDeleteListener.class);

    @Autowired
    private ItemSearchService itemSearchService;

    /**
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            Long[] goodsIds = (Long[]) objectMessage.getObject();
            logger.info("监听获取到消息：" + goodsIds);
            itemSearchService.deleteByGoodsIds(Arrays.asList(goodsIds));
            logger.info("执行索引库删除");
        } catch (JMSException e) {
            logger.error("商品搜索索引删除监听处理失败", e);
        }
    }
}
