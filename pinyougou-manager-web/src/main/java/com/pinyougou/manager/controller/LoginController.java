package com.pinyougou.manager.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录功能
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    /**
     * 获取当前登录用户名
     *
     * @return 用户名信息
     */
    @GetMapping("/showName")
    @ResponseBody
    public Map showName() {
        Map map = new HashMap();
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        map.put("username", user);
        return map;
    }

    /**
     * 测试接口
     *
     * @return 测试数据
     */
    @GetMapping("/test")
    @ResponseBody
    public String demo() {
        return "test_data";
    }
}
