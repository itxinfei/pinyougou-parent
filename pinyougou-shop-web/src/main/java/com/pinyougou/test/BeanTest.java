package com.pinyougou.test;

import com.pinyougou.pojo.TbGoods;
import com.pinyougou.shop.controller.GoodsController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * junit测试Controller
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/springmvc.xml"})
public class BeanTest {
    @Autowired
    private GoodsController goodsController;

    @Test
    public void findAllTest() {
        List<TbGoods> all = goodsController.findAll();
        for (TbGoods tbGoods : all) {
            System.out.println(tbGoods.getId());
        }
    }
}
