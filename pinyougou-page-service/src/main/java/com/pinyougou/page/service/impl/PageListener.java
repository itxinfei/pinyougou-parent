package com.pinyougou.page.service.impl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pinyougou.page.service.ItemPageService;

/**
 * 监听类（用于生成网页）
 *
 * @author Administrator
 */
@Component
public class PageListener implements MessageListener {

    private static final Logger logger = Logger.getLogger(PageListener.class);

    @Autowired
    private ItemPageService itemPageService;

    /**
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
        try {
            String text = textMessage.getText();
            logger.info("接收到消息：" + text);
            boolean b = itemPageService.genItemHtml(Long.parseLong(text));
            logger.info("网页生成结果：" + b);

        } catch (JMSException e) {
            logger.error("页面生成监听处理失败", e);
        }
    }
}
