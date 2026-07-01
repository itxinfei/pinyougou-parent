package com.pinyougou.user.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.user.service.LoginService;

import entity.Result;

/**
 * 登录控制器
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/login")
public class LoginController {

    @Reference
    private LoginService loginService;

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @RequestMapping("/loginByUsername")
    public Result loginByUsername(String username, String password) {
        Map<String, Object> result = loginService.loginByUsername(username, password);

        boolean success = (boolean) result.get("success");
        String message = (String) result.get("message");

        if (success) {
            // 登录成功，返回token和用户信息
            return Result.buildResult(success, message, result);
        } else {
            // 登录失败
            return new Result(success, message);
        }
    }

    /**
     * 手机号验证码登录
     *
     * @param phone 手机号
     * @param code 验证码
     * @return 登录结果
     */
    @RequestMapping("/loginBySms")
    public Result loginBySms(String phone, String code) {
        Map<String, Object> result = loginService.loginBySms(phone, code);

        boolean success = (boolean) result.get("success");
        String message = (String) result.get("message");

        if (success) {
            return Result.buildResult(success, message, result);
        } else {
            return new Result(success, message);
        }
    }

    /**
     * 用户注册
     *
     * @param userJson 用户信息（JSON格式）
     * @param smscode 短信验证码
     * @return 注册结果
     */
    @RequestMapping("/register")
    public Result register(String userJson, String smscode) {
        try {
            // 解析JSON字符串为TbUser对象
            TbUser user = JSON.parseObject(userJson, TbUser.class);

            // 验证必要字段
            if (user.getUsername() == null || user.getPassword() == null) {
                return new Result(false, "用户名和密码不能为空");
            }

            // 调用注册服务
            Map<String, Object> result = loginService.register(user, smscode);

            boolean success = (boolean) result.get("success");
            String message = (String) result.get("message");

            return new Result(success, message);
        } catch (Exception e) {
            return new Result(false, "注册失败：" + e.getMessage());
        }
    }

    /**
     * 根据Token获取用户信息
     *
     * @param token JWT Token
     * @return 用户信息
     */
    @RequestMapping("/getUserInfo")
    public Result getUserInfo(String token) {
        Map<String, Object> result = loginService.getUserByToken(token);

        boolean success = (boolean) result.get("success");
        String message = (String) result.get("message");

        if (success) {
            return Result.buildResult(success, message, result.get("userInfo"));
        } else {
            return new Result(success, message);
        }
    }

    /**
     * 登出（作废Token）
     *
     * @param token JWT Token
     * @return 登出结果
     */
    @RequestMapping("/logout")
    public Result logout(String token) {
        try {
            Map<String, Object> result = loginService.logout(token);

            boolean success = (boolean) result.get("success");
            String message = (String) result.get("message");

            return new Result(success, message);
        } catch (Exception e) {
            logger.error("登出失败", e);
            return new Result(false, "登出失败");
        }
    }
}
