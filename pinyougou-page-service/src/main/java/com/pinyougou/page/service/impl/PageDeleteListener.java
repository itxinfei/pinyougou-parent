package com.pinyougou.page.service.impl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pinyougou.page.service.ItemPageService;

@Component
public class PageDeleteListener implements MessageListener {

    private static final Logger logger = Logger.getLogger(PageDeleteListener.class);

    @Autowired
    private ItemPageService itemPageService;

    /**
     * @param message
     */
    @Override
    public void onMessage(Message message) {

        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            Long[] goodsIds = (Long[]) objectMessage.getObject();
            logger.info("接收到消息:" + goodsIds);
            boolean b = itemPageService.deleteItemHtml(goodsIds);
            logger.info("删除网页：" + b);

        } catch (JMSException e) {
            logger.error("页面删除监听处理失败", e);
        }


    }

}
