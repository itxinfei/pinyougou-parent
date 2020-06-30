package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.sellergoods.service.GoodsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * controller
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Reference
    private GoodsService goodsService;


}
