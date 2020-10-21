package com.pinyougou.pay.service;

import java.util.Map;

public interface WeixinPayService {

    /**
     * 生成二维码
     *
     * @param out_trade_no
     * @param total_fee
     * @return
     */
    Map createNative(String out_trade_no, String total_fee);

    /**
     * 查询支付订单状态
     *
     * @param out_trade_no
     * @return
     */
    Map queryPayStatus(String out_trade_no);

}
