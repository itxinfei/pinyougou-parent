package com.pinyougou.user.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.pojo.TbUserExample.Criteria;
import com.pinyougou.user.service.LoginService;
import com.pinyougou.user.service.UserService;

import entity.Result;
import util.JwtUtils;

/**
 * 登录服务实现类（简化版 - 不使用Spring Security）
 *
 * @author Administrator
 */
@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger logger = Logger.getLogger(LoginServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private TbUserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户名密码登录
     */
    @Override
    public Map<String, Object> loginByUsername(String username, String password) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 根据用户名查询用户
            TbUserExample example = new TbUserExample();
            Criteria criteria = example.createCriteria();
            criteria.andUsernameEqualTo(username);
            criteria.andStatusEqualTo("1"); // 状态为正常

            List<TbUser> userList = userMapper.selectByExample(example);

            if (userList.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("message", "用户名或密码错误");
                return resultMap;
            }

            TbUser user = userList.get(0);

            // 2. 验证密码（MD5加密后比对）
            String md5Password = DigestUtils.md5Hex(password);
            if (!user.getPassword().equals(md5Password)) {
                resultMap.put("success", false);
                resultMap.put("message", "用户名或密码错误");
                return resultMap;
            }

            // 3. 生成JWT token
            String token = jwtUtils.generateToken(username);

            // 4. 将token存入Redis（用于登出时作废）
            redisTemplate.boundValueOps("token:" + username).set(token);

            // 5. 返回用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("phone", user.getPhone());
            userInfo.put("nickName", user.getNickName());
            userInfo.put("headPic", user.getHeadPic());

            resultMap.put("success", true);
            resultMap.put("message", "登录成功");
            resultMap.put("token", token);
            resultMap.put("userInfo", userInfo);

            logger.info("用户登录成功: " + username);

        } catch (Exception e) {
            logger.error("登录失败: " + username, e);
            resultMap.put("success", false);
            resultMap.put("message", "登录失败，请稍后重试");
        }

        return resultMap;
    }

    /**
     * 手机号验证码登录
     */
    @Override
    public Map<String, Object> loginBySms(String phone, String code) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 校验验证码
            boolean checkSmsCode = userService.checkSmsCode(phone, code);
            if (!checkSmsCode) {
                resultMap.put("success", false);
                resultMap.put("message", "验证码错误或已过期");
                return resultMap;
            }

            // 2. 根据手机号查询用户
            TbUserExample example = new TbUserExample();
            Criteria criteria = example.createCriteria();
            criteria.andPhoneEqualTo(phone);
            criteria.andStatusEqualTo("1"); // 状态为正常

            List<TbUser> userList = userMapper.selectByExample(example);

            TbUser user;
            if (userList.isEmpty()) {
                // 3. 用户不存在，自动注册
                user = new TbUser();
                user.setPhone(phone);
                user.setUsername("user_" + phone); // 生成用户名
                user.setPassword(DigestUtils.md5Hex(UUID.randomUUID().toString())); // 随机密码
                user.setNickName("用户" + phone.substring(phone.length() - 4)); // 昵称
                user.setStatus("1"); // 正常状态
                user.setSourceType("1"); // 注册来源
                user.setIsMobileCheck("1"); // 手机已验证
                user.setCreated(new Date());
                user.setUpdated(new Date());

                userMapper.insert(user);
                logger.info("用户自动注册: " + phone);
            } else {
                user = userList.get(0);
            }

            // 4. 生成JWT token
            String token = jwtUtils.generateToken(user.getUsername());
            redisTemplate.boundValueOps("token:" + user.getUsername()).set(token);

            // 5. 返回用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("phone", user.getPhone());
            userInfo.put("nickName", user.getNickName());
            userInfo.put("headPic", user.getHeadPic());

            resultMap.put("success", true);
            resultMap.put("message", "登录成功");
            resultMap.put("token", token);
            resultMap.put("userInfo", userInfo);

            logger.info("用户短信登录成功: " + phone);

        } catch (Exception e) {
            logger.error("短信登录失败: " + phone, e);
            resultMap.put("success", false);
            resultMap.put("message", "登录失败，请稍后重试");
        }

        return resultMap;
    }

    /**
     * 根据token获取用户信息
     */
    @Override
    public Map<String, Object> getUserByToken(String token) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 验证token有效性
            if (!jwtUtils.validateToken(token)) {
                resultMap.put("success", false);
                resultMap.put("message", "token无效或已过期");
                return resultMap;
            }

            // 2. 从token中获取用户名
            String username = jwtUtils.getUsernameFromToken(token);

            // 3. 验证token是否在Redis中（防止伪造）
            String redisToken = (String) redisTemplate.boundValueOps("token:" + username).get();
            if (redisToken == null || !redisToken.equals(token)) {
                resultMap.put("success", false);
                resultMap.put("message", "token已失效");
                return resultMap;
            }

            // 4. 获取用户信息
            TbUser user = getUserByUsername(username);
            if (user == null) {
                resultMap.put("success", false);
                resultMap.put("message", "用户不存在");
                return resultMap;
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("phone", user.getPhone());
            userInfo.put("nickName", user.getNickName());
            userInfo.put("headPic", user.getHeadPic());

            resultMap.put("success", true);
            resultMap.put("userInfo", userInfo);

        } catch (Exception e) {
            logger.error("根据token获取用户信息失败", e);
            resultMap.put("success", false);
            resultMap.put("message", "获取用户信息失败");
        }

        return resultMap;
    }

    /**
     * 用户注册
     */
    @Override
    public Map<String, Object> register(TbUser user, String smscode) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 校验验证码
            boolean checkSmsCode = userService.checkSmsCode(user.getPhone(), smscode);
            if (!checkSmsCode) {
                resultMap.put("success", false);
                resultMap.put("message", "验证码错误或已过期");
                return resultMap;
            }

            // 2. 检查手机号是否已注册
            TbUserExample example = new TbUserExample();
            Criteria criteria = example.createCriteria();
            criteria.andPhoneEqualTo(user.getPhone());

            List<TbUser> userList = userMapper.selectByExample(example);
            if (!userList.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("message", "该手机号已注册");
                return resultMap;
            }

            // 3. 检查用户名是否已存在
            TbUserExample usernameExample = new TbUserExample();
            Criteria usernameCriteria = usernameExample.createCriteria();
            usernameCriteria.andUsernameEqualTo(user.getUsername());

            List<TbUser> usernameList = userMapper.selectByExample(usernameExample);
            if (!usernameList.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("message", "用户名已存在");
                return resultMap;
            }

            // 4. 密码加密
            user.setPassword(DigestUtils.md5Hex(user.getPassword()));
            user.setStatus("1");
            user.setSourceType("1");
            user.setIsMobileCheck("1");
            user.setCreated(new Date());
            user.setUpdated(new Date());

            // 5. 保存用户
            userMapper.insert(user);

            resultMap.put("success", true);
            resultMap.put("message", "注册成功");

            logger.info("用户注册成功: " + user.getUsername());

        } catch (Exception e) {
            logger.error("用户注册失败: " + user.getUsername(), e);
            resultMap.put("success", false);
            resultMap.put("message", "注册失败，请稍后重试");
        }

        return resultMap;
    }

    /**
     * 根据用户名查询用户
     */
    private TbUser getUserByUsername(String username) {
        TbUserExample example = new TbUserExample();
        Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);

        List<TbUser> userList = userMapper.selectByExample(example);
        return userList.isEmpty() ? null : userList.get(0);
    }
}
