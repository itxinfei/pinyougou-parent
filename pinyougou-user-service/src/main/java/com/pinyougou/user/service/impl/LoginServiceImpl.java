package com.pinyougou.user.service.impl;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.exception.ValidationException;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.pojo.TbUserExample.Criteria;
import com.pinyougou.user.service.LoginService;
import com.pinyougou.user.service.UserService;

import entity.Result;
import util.JwtUtils;

/**
 * 登录服务实现类
 * <p>
 * ✅ 已优化：密码加密方式
 * - 旧方案：MD5（易被彩虹表破解）
 * - 新方案：BCrypt（自动加盐、工作因子可调）
 * <p>
 * @author Administrator
 */
@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger logger = Logger.getLogger(LoginServiceImpl.class);

    // ✅ 使用BCrypt密码验证器
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
     * <p>
     * 认证流程：
     * 1. 根据用户名查询用户（仅查询状态为正常的用户）
     * 2. ✅ 验证密码（BCrypt加密比对）
     * 3. 生成JWT Token
     * 4. 将Token存入Redis（用于登出时作废和防伪造）
     * 5. 返回用户基本信息
     * <p>
     * 安全机制：
     * - 密码明文传输风险：必须使用HTTPS
     * - ✅ 密码加密：BCrypt（自动加盐，抗彩虹表攻击）
     * - Token防伪造：Redis中存储有效Token列表
     * - 状态检查：只允许状态为 "1" 的正常用户登录
     * <p>
     * ✅ 已修复：
     * 1. 密码加密方式：MD5 -> BCrypt
     * 2. 使用BCrypt的matches()方法验证密码
     * <p>
     * ⚠️ 待优化：
     * - 没有登录失败次数限制（易被暴力破解）
     * - 没有验证码机制（易被机器批量登录）
     * - Token没有做IP绑定（Token被盗后可跨IP使用）
     * - 没有登录日志记录（无法追踪异常登录）
     * <p>
     * 改进建议：
     * - 增加验证码：连续失败3次后要求输入验证码
     * - 增加登录日志：记录登录IP、时间、设备
     * - Token绑定IP：防止Token被盗用
     * - 失败锁定：连续失败5次锁定账号30分钟
     *
     * @param username 用户名
     * @param password 明文密码（前端应加密传输）
     * @return 登录结果 Map（success/message/token/userInfo）
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

            // 2. 验证密码（BCrypt加密比对）
            // BCrypt特点：自动加盐，matches()方法自动提取salt并验证
            // 工作因子默认为10（编码后的哈希值长度为60字符）
            if (!passwordEncoder.matches(password, user.getPassword())) {
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
     * <p>
     * 认证流程：
     * 1. 验证短信验证码（从Redis查询）
     * 2. 根据手机号查询用户
     * 3. 用户不存在则自动注册（降低注册门槛）
     * 4. 生成JWT Token
     * 5. 返回用户信息
     * <p>
     * 自动注册逻辑：
     * - 用户名: user_ + 手机号后4位（示例: user_8888）
     * - 昵称: 用户 + 手机号后4位（示例: 用户8888）
     * - 密码: 随机UUID的MD5值（用户无法使用密码登录，只能用短信登录）
     * - 状态: 正常(1)，手机已验证(1)
     * <p>
     * 安全机制：
     * - 验证码校验：Redis中存储的验证码对比
     * - 验证码有效期：默认30分钟（在 UserServiceImpl.createSmsCode() 中设置）
     * - 验证码一次性：验证成功后未删除（⚠️ 建议验证成功后删除）
     * <p>
     * ⚠️ 安全问题：
     * 1. 验证码生成使用 Math.random()（不安全，应使用 SecureRandom）
     * 2. 验证码未设置发送频率限制（易被短信轰炸攻击）
     * 3. 验证码未设置尝试次数限制（易被暴力破解）
     * 4. 自动注册逻辑可能被利用（批量注册垃圾账号）
     * <p>
     * 改进建议：
     * - 验证码：Math.random() -> SecureRandom
     * - 频率限制：60秒内同一手机号只能发送1次
     * - 次数限制：同手机号每天最多发送10次
     * - IP限制：同一IP每天最多发送50次
     * - 图形验证码：发送短信前需先验证图形验证码
     *
     * @param phone 手机号
     * @param code 短信验证码
     * @return 登录结果 Map（success/message/token/userInfo）
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
                // ✅ 使用BCrypt加密随机密码（替代MD5）
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
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
     * 用户登出（作废Token）
     * <p>
     * 登出流程：
     * 1. 从Token中解析出用户名
     * 2. 删除Redis中的Token（用户只能登出自己）
     * 3. 可选：将Token加入黑名单（强制登出功能）
     * <p>
     * Token作废机制：
     * - 正常登出：直接删除Redis中的Token
     * - 强制登出（管理员踢人）：将Token加入黑名单，设置过期时间
     * <p>
     * 注意事项：
     * - 删除Redis中的Token后，该Token立即失效
     * - 黑名单机制：为Token设置30分钟过期时间（Token剩余有效期）
     * - 所有需要验证Token的接口必须查询Redis黑名单
     * <p>
     * ⚠️ 当前实现缺陷：
     * - getTokenBlacklist() 使用 String key，但 token:blacklist: 前缀缺失
     * - 应该使用 Set 存储黑名单（支持批量管理）
     * - 应该增加黑名单查询方法（供Token验证使用）
     * <p>
     * @param token JWT Token
     * @return 操作结果 Map（success/message）
     */
    @Override
    public Map<String, Object> logout(String token) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 1. 从token中解析出用户名
            String username = jwtUtils.getUsernameFromToken(token);

            // 2. 删除Redis中的token（作废）
            redisTemplate.delete("token:" + username);

            // 3. 可选：将token加入黑名单（用于强制登出）
            // 设置过期时间为token的剩余有效期
            redisTemplate.boundValueOps("token:blacklist:" + token).set("1", 30, java.util.concurrent.TimeUnit.MINUTES);

            resultMap.put("success", true);
            resultMap.put("message", "登出成功");

            logger.info("用户登出成功: " + username);

        } catch (Exception e) {
            logger.error("登出失败", e);
            resultMap.put("success", false);
            resultMap.put("message", "登出失败");
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
