package com.pinyougou.user.service;

import java.util.Map;

import com.pinyougou.pojo.TbUser;

/**
 * 用户登录服务接口
 * <p>
 * 核心功能：
 * - 用户名密码登录
 * - 手机号验证码登录
 * - 图形验证码（防暴力破解）
 * - Token验证和刷新
 * - 用户注册
 * - 登出
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

    /**
     * 登出（作废Token）
     *
     * @param token JWT Token
     * @return 登出结果
     */
    public Map<String, Object> logout(String token);

    // ========== 图形验证码 ==========

    /**
     * 生成图形验证码
     * <p>
     * 用途：
     * - 登录时防止暴力破解
     * - 发送短信前验证
     * <p>
     * 实现方式：
     * - 使用Kaptcha或EasyCaptcha生成
     * - 验证码文本存入Redis（5分钟过期）
     * - 验证码图片转为Base64返回
     *
     * @param key 唯一标识（用于Redis存储和验证）
     * @return Base64编码的图片
     */
    public String generateCaptcha(String key);

    /**
     * 验证图形验证码
     *
     * @param key 唯一标识
     * @param code 用户输入的验证码
     * @return true-验证成功，false-验证失败
     */
    public boolean verifyCaptcha(String key, String code);

}
