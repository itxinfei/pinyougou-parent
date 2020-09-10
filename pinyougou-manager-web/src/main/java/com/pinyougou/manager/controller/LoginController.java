package com.pinyougou.manager.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录功能
 */
@Controller
@RequestMapping("/login")
@SuppressWarnings("unchecked")//使用了未经检查或不安全的操作。
public class LoginController {
    /**
     * 账号登录功能
     *
     * @return
     */
    @RequestMapping("/showName")
    public Map showName(String username, String password) {
        System.out.println(username + password);
        Map map = new HashMap();
        // 获得用户名信息:
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        map.put("username", user);
        //后台输出用户
        System.out.println(map.toString());
        return map;
    }

    /**
     * 测试数据
     *
     * @return
     */
    @RequestMapping("/test")
    @ResponseBody
    public String demo() {
        return "测试数据";
    }
}
