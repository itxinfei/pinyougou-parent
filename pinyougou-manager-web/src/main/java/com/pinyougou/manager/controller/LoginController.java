package com.pinyougou.manager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录功能
 */
@RestController
@RequestMapping("/login")
public class LoginController {
    /**
     *  账号登录功能
     * @return
     */
   /* @RequestMapping("/showName")
    public Map name() {
        String name = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Map map = new HashMap();
        map.put("loginName", name);
        System.out.println(map);
        return map;
    }*/

}
