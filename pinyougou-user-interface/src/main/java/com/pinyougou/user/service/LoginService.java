package com.pinyougou.user.service;

import java.util.Map;

import com.pinyougou.pojo.TbUser;

/**
 * 用户登录服务接口
 *
 * @author Administrator
 */
public interface LoginService {

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（包含token）
     */
    public Map<String, Object> loginByUsername(String username, String password);

    /**
     * 手机号验证码登录
     *
     * @param phone 手机号
     * @param code 验证码
     * @return 登录结果（包含token）
     */
    public Map<String, Object> loginBySms(String phone, String code);

    /**
     * 根据token获取用户信息
     *
     * @param token JWT token
     * @return 用户信息
     */
    public Map<String, Object> getUserByToken(String token);

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @param smscode 短信验证码
     * @return 注册结果
     */
    public Map<String, Object> register(TbUser user, String smscode);
}
